package io.github.shaksternano.gifcodec

import kotlinx.io.Sink
import kotlinx.io.writeString
import kotlin.math.ceil
import kotlin.math.log2
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

private const val GIF_MAX_COLORS: Int = 256
private const val GIF_MINIMUM_COLOR_TABLE_SIZE: Int = 2
private const val GIF_MINIMUM_FRAME_DURATION_CENTISECONDS: Int = 2
internal const val GIF_MAX_BLOCK_SIZE: Int = 0xFF

/*
 * Reference:
 * https://www.matthewflickinger.com/lab/whatsinagif/bits_and_bytes.asp
 */
class GifEncoder(
    private val sink: Sink,
    private val loopCount: Int = 0,
    maxColors: Int = GIF_MAX_COLORS,
    private val colorTolerance: Double = 0.0,
    private val alphaFill: Int = -1,
    private val comment: String = "",
    private val minimumFrameDurationCentiseconds: Int = GIF_MINIMUM_FRAME_DURATION_CENTISECONDS,
    private val quantizer: ColorQuantizer = NeuQuantizer.DEFAULT,
) : AutoCloseable {

    init {
        require(minimumFrameDurationCentiseconds > 0) {
            "Minimum frame duration must be positive: $minimumFrameDurationCentiseconds"
        }
    }

    private val maxColors: Int = maxColors.coerceIn(1, GIF_MAX_COLORS)
    private val minimumFrameDuration: Duration = minimumFrameDurationCentiseconds.centiseconds
    private var initialized: Boolean = false
    private var width: Int? = null
    private var height: Int? = null
    private lateinit var previousFrame: Image
    private var pendingWrite: Image? = null
    private var pendingDuration: Duration = Duration.ZERO
    private var pendingDisposalMethod: DisposalMethod = DisposalMethod.UNSPECIFIED
    private var frameCount: Int = 0

    private fun init(width: Int, height: Int, loopCount: Int) {
        if (initialized) return
        this.width = width
        this.height = height
        sink.writeGifHeader()
        sink.writeGifLogicalScreenDescriptor(width, height)
        sink.writeGifApplicationExtension(loopCount)
        sink.writeGifCommentExtension(comment)
        initialized = true
    }

    fun writeFrame(
        image: IntArray,
        width: Int,
        height: Int,
        duration: Duration,
    ) {
        val targetWidth = this.width ?: width
        val targetHeight = this.height ?: height
        /*
         * Make sure all frames have the same dimensions
         * by cropping or padding with transparent pixels,
         * and remove any partial alpha, as GIFs do not
         * support partial transparency.
         */
        val currentFrame = Image(image, width, height)
            .cropOrPad(targetWidth, targetHeight)
            .fillPartialAlpha(alphaFill)
        if (::previousFrame.isInitialized && previousFrame.isSimilar(currentFrame, colorTolerance)) {
            // Merge similar sequential frames into one
            pendingDuration += duration
            return
        }

        // Optimise transparency
        val toWrite: Image
        val disposalMethod: DisposalMethod
        if (!::previousFrame.isInitialized) {
            // First frame
            toWrite = currentFrame
            disposalMethod = DisposalMethod.DO_NOT_DISPOSE
        } else {
            // Subsequent frames
            val optimized = optimizeTransparency(previousFrame, currentFrame, colorTolerance)
            if (optimized == null) {
                pendingDisposalMethod = DisposalMethod.RESTORE_TO_BACKGROUND_COLOR
                toWrite = currentFrame
                disposalMethod = DisposalMethod.RESTORE_TO_BACKGROUND_COLOR
            } else {
                toWrite = optimized
                disposalMethod = DisposalMethod.DO_NOT_DISPOSE
            }
        }

        /*
         * Write the previous frame if it exists and the
         * duration is long enough. The previous frame and
         * the current frame have already been established
         * to be different.
         */
        val pendingWrite1 = pendingWrite
        if (pendingWrite1 != null && pendingDuration >= minimumFrameDuration) {
            val centiseconds = pendingDuration.roundedUpCentiseconds
            initAndWriteFrame(pendingWrite1, centiseconds)
            // Might end up being negative
            pendingDuration -= centiseconds.centiseconds
            pendingWrite = null
        }

        val pendingWrite2 = pendingWrite
        if (pendingWrite2 == null) {
            /*
             * Don't write frame just yet, as the
             * frame's duration will be decided by
             * whether subsequent frames are similar
             * or not.
             */
            previousFrame = currentFrame
            pendingWrite = toWrite
            pendingDuration += duration
            pendingDisposalMethod = disposalMethod
        } else {
            /*
             * Handle the minimum frame duration.
             * Pending duration is already established
             * to be less than the minimum frame duration.
             */
            val newPendingDuration = pendingDuration + duration
            if (newPendingDuration < minimumFrameDuration) {
                pendingDuration = newPendingDuration
            } else {
                initAndWriteFrame(pendingWrite2, minimumFrameDurationCentiseconds)
                previousFrame = currentFrame
                pendingWrite = toWrite
                pendingDuration = newPendingDuration - minimumFrameDuration
                pendingDisposalMethod = disposalMethod
            }
        }
    }

    private fun initAndWriteFrame(
        image: Image,
        durationCentiseconds: Int,
        loopCount: Int = this.loopCount,
    ) {
        initAndWriteFrame(
            image,
            durationCentiseconds,
            pendingDisposalMethod,
            loopCount,
        )
    }

    private fun initAndWriteFrame(
        image: Image,
        durationCentiseconds: Int,
        disposalMethod: DisposalMethod,
        loopCount: Int,
    ) {
        init(image.width, image.height, loopCount)
        val data = getImageData(image)
        sink.writeGifImage(
            data,
            image.width,
            image.height,
            durationCentiseconds,
            disposalMethod,
        )
        frameCount++
    }

    private fun getImageData(image: Image): QuantizedImageData {
        // Build color table
        val argb = image.argb
        val rgb = mutableListOf<Byte>()
        val distinctColors = mutableSetOf<Int>()
        var hasTransparent = false
        argb.forEach { pixel ->
            val alpha = pixel ushr 24
            if (alpha == 0) {
                hasTransparent = true
            } else {
                val red = pixel shr 16 and 0xFF
                val green = pixel shr 8 and 0xFF
                val blue = pixel and 0xFF
                rgb.add(red.toByte())
                rgb.add(green.toByte())
                rgb.add(blue.toByte())
                distinctColors.add(pixel)
            }
        }
        val distinctColorCount = distinctColors.size + if (hasTransparent) 1 else 0
        val colorCount = distinctColorCount.coerceAtMost(maxColors)
        val colorTableSize = colorCount.roundUpPowerOf2()
            .coerceAtLeast(GIF_MINIMUM_COLOR_TABLE_SIZE)
        val quantizer = if (distinctColorCount > maxColors) {
            quantizer
        } else {
            DirectColorQuantizer
        }
        val quantizerMaxColors =
            if (hasTransparent) colorCount - 1
            else colorCount
        val quantizationResult = quantizer.quantize(
            rgb.toByteArray(),
            quantizerMaxColors,
        )
        val quantizedColors = quantizationResult.colors
        val colorTable: ByteArray
        val transparentColorIndex: Int
        if (hasTransparent) {
            colorTable = ByteArray(colorTableSize * 3)
            quantizedColors.copyInto(
                colorTable,
                // First three bytes are reserved for transparent color
                destinationOffset = 3,
            )
            transparentColorIndex = 0
        } else {
            if (colorCount == colorTableSize) {
                colorTable = quantizedColors
            } else {
                colorTable = ByteArray(colorTableSize * 3)
                quantizedColors.copyInto(colorTable)
            }
            transparentColorIndex = -1
        }

        // Get color indices
        val imageColorIndices = ByteArray(argb.size)
        // First index is reserved for transparent color
        val indexOffset = if (hasTransparent) 1 else 0
        argb.forEachIndexed { i, pixel ->
            val index = if (colorCount == 1) {
                0
            } else {
                val alpha = pixel ushr 24
                if (alpha == 0) {
                    0
                } else {
                    val red = pixel shr 16 and 0xFF
                    val green = pixel shr 8 and 0xFF
                    val blue = pixel and 0xFF
                    quantizationResult.getColorIndex(red, green, blue) + indexOffset
                }
            }
            imageColorIndices[i] = index.toByte()
        }

        return QuantizedImageData(
            colorTable,
            imageColorIndices,
            colorTableSize,
            transparentColorIndex,
        )
    }

    override fun close() {
        val pendingWrite = pendingWrite
        if (pendingWrite != null) {
            if (frameCount < 2) {
                pendingDisposalMethod = DisposalMethod.UNSPECIFIED
            }
            val centiseconds = pendingDuration.roundedUpCentiseconds
                .coerceAtLeast(minimumFrameDurationCentiseconds)
            val loopCount = if (this.frameCount > 1) loopCount else -1
            initAndWriteFrame(
                pendingWrite,
                centiseconds,
                loopCount,
            )
        }
        sink.writeGifTrailer()
        sink.close()
    }
}

private val Int.centiseconds: Duration
    get() = (this * 10).milliseconds

private val Duration.roundedUpCentiseconds: Int
    get() = ceil(inWholeMilliseconds / 10.0).toInt()

@Suppress("ArrayInDataClass")
private data class QuantizedImageData(
    val colorTable: ByteArray,
    val imageColorIndices: ByteArray,
    val colorTableSize: Int,
    val transparentColorIndex: Int,
)

private fun optimizeTransparency(
    previousImage: Image,
    currentImage: Image,
    colorTolerance: Double,
): Image? {
    if (previousImage.width != currentImage.width || previousImage.height != currentImage.height) {
        return null
    }
    val optimizedPixels = IntArray(currentImage.argb.size)
    previousImage.argb.zip(currentImage.argb).forEachIndexed { i, (previousArgb, currentArgb) ->
        val previousAlpha = previousArgb ushr 24
        val currentAlpha = currentArgb ushr 24
        if (currentAlpha == 0 && previousAlpha != 0) {
            /*
             * Current frame has a transparent pixel where
             * the previous frame had an opaque pixel
             */
            return null
        }
        val colorDistance = colorDistance(previousArgb, currentArgb)
        optimizedPixels[i] = if (colorDistance > colorTolerance) {
            currentArgb
        } else {
            0
        }
    }
    return Image(optimizedPixels, currentImage.width, currentImage.height)
}

internal fun Sink.writeGifHeader() {
    writeString("GIF89a")
}

internal fun Sink.writeGifLogicalScreenDescriptor(width: Int, height: Int) {
    writeLittleEndianShort(width)
    writeLittleEndianShort(height)
    writeByte(0x00) // Global color table packed field
    writeByte(0x00) // Background color index
    writeByte(0x00) // Pixel aspect ratio
}

internal fun Sink.writeGifColorTable(colorTable: ByteArray) {
    write(colorTable)
}

internal fun Sink.writeGifApplicationExtension(loopCount: Int) {
    // No looping
    if (loopCount < 0) {
        return
    }

    writeByte(0x21)            // Extension introducer
    writeByte(0xFF)            // Application extension label
    writeByte(0x0B)            // Length of Application block, 11 bytes
    writeString("NETSCAPE2.0") // Application identifier
    writeByte(0x03)            // Length of data sub-block, 3 bytes
    writeByte(0x01)            // Constant
    writeLittleEndianShort(loopCount)
    writeByte(0x00)            // Block Terminator
}

internal fun Sink.writeGifCommentExtension(comment: String) {
    if (comment.isEmpty()) {
        return
    }

    writeByte(0x21) // Extension introducer
    writeByte(0xFE) // Comment extension label
    writeGifSubBlocks(comment.encodeToByteArray())
    writeByte(0x00) // Block Terminator
}

private fun Sink.writeGifImage(
    data: QuantizedImageData,
    width: Int,
    height: Int,
    durationCentiseconds: Int,
    disposalMethod: DisposalMethod,
) {
    val (colorTable, imageColorIndices, colorTableSize, transparentColorIndex) = data
    writeGifGraphicsControlExtension(disposalMethod, durationCentiseconds, transparentColorIndex)
    writeGifImageDescriptor(width, height, colorTableSize)
    writeGifColorTable(colorTable)
    writeGifImageData(imageColorIndices, colorTableSize)
}

internal fun Sink.writeGifGraphicsControlExtension(
    disposalMethod: DisposalMethod,
    durationCentiseconds: Int,
    transparentColorIndex: Int,
) {
    writeByte(0x21) // Extension code
    writeByte(0xF9) // Graphics Control Label
    writeByte(0x04) // Block size
    val transparentColorFlag = if (transparentColorIndex < 0) 0 else 1
    /*
     * Bits:
     * 1-3 : Reserved for future use (unused, set to 0)
     * 4-6 : Disposal method
     * 7   : User input flag         (unused, set to 0)
     * 8   : Transparent color flag
     */
    val packed = (disposalMethod.id shl 2 or transparentColorFlag) and 0b00011101
    writeByte(packed)
    writeLittleEndianShort(durationCentiseconds)
    writeByte(transparentColorIndex.coerceAtLeast(0))
    writeByte(0x00) // Block terminator
}

internal fun Sink.writeGifImageDescriptor(width: Int, height: Int, localColorTableSize: Int) {
    writeByte(0x2C) // Image Separator
    writeInt(0)     // Image left and top positions, two bytes each
    writeLittleEndianShort(width)
    writeLittleEndianShort(height)
    /*
     * Bits:
     * 1   : Local color table flag
     * 2   : Interlace flag          (unused, set to 0)
     * 3   : Sort flag               (unused, set to 0)
     * 4-5 : Reserved for future use (unused, set to 0)
     * 6-8 : Local color table size
     */
    val packed = (0b10000000 or getColorTableRepresentedSize(localColorTableSize)) and 0b10000111
    writeByte(packed)
}

internal fun Sink.writeGifImageData(imageColorIndices: ByteArray, colorTableSize: Int) {
    writeLzwIndexStream(imageColorIndices.asList(), colorTableSize)
}

internal fun Sink.writeGifTrailer() {
    writeByte(0x3B)
}

private fun Sink.writeGifSubBlocks(bytes: ByteArray) {
    bytes.asList()
        .chunked(GIF_MAX_BLOCK_SIZE)
        .forEach {
            writeByte(it.size)      // Number of bytes of data in sub-block
            write(it.toByteArray()) // Sub-block data
        }
}

private fun getColorTableRepresentedSize(maxColors: Int): Int =
    ceil(log2(maxColors.toDouble())).toInt() - 1
