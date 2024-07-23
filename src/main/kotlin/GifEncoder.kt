package io.github.shaksternano.gifcodec

import kotlinx.io.Sink
import kotlinx.io.writeString
import kotlin.math.ceil
import kotlin.math.log2
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

private const val GIF_MAX_COLORS: Int = 256
private const val GIF_MINIMUM_COLOR_TABLE_SIZE: Int = 2
internal const val GIF_MAX_BLOCK_SIZE: Int = 0xFF
private val GIF_MINIMUM_FRAME_DURATION: Duration = 20.milliseconds

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
    private val minimumFrameDuration: Duration = GIF_MINIMUM_FRAME_DURATION,
    private val quantizer: ColorQuantizer = NeuQuantizer.DEFAULT,
) : AutoCloseable {

    private val maxColors: Int = maxColors.coerceIn(1, GIF_MAX_COLORS)
    private var initialized: Boolean = false
    private lateinit var previousFrame: Image
    private var pendingWrite: Image? = null
    private var pendingDuration: Duration = Duration.ZERO
    private var pendingDisposalMethod: DisposalMethod = DisposalMethod.UNSPECIFIED
    private var frameCount: Int = 0

    private fun init(width: Int, height: Int, loopCount: Int) {
        if (initialized) return
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
        val currentFrame = Image(image, width, height).fillPartialAlpha(alphaFill)
        if (::previousFrame.isInitialized && previousFrame.isSimilar(currentFrame, colorTolerance)) {
            // Merge similar sequential frames into one
            pendingDuration += duration
            return
        }

        // Optimise transparency.
        val (toWrite, disposalMethod) = if (!::previousFrame.isInitialized) {
            // First frame
            currentFrame to DisposalMethod.DO_NOT_DISPOSE
        } else {
            // Subsequent frames
            val optimized = optimizeTransparency(previousFrame, currentFrame, colorTolerance)
            if (optimized == null) {
                pendingDisposalMethod = DisposalMethod.RESTORE_TO_BACKGROUND_COLOR
                currentFrame to DisposalMethod.RESTORE_TO_BACKGROUND_COLOR
            } else {
                optimized to DisposalMethod.DO_NOT_DISPOSE
            }
        }

        // Write the previous frame if it exists and the duration is long enough.
        val pendingWrite1 = pendingWrite
        if (pendingWrite1 != null && pendingDuration >= minimumFrameDuration) {
            initAndWriteFrame(pendingWrite1)
            pendingWrite = null
        }

        val pendingWrite2 = pendingWrite
        if (pendingWrite2 == null) {
            previousFrame = currentFrame
            pendingWrite = toWrite
            pendingDuration = duration
            pendingDisposalMethod = disposalMethod
        } else {
            // Handle the minimum frame duration.
            val remainingDuration = minimumFrameDuration - pendingDuration
            if (remainingDuration < duration) {
                initAndWriteFrame(pendingWrite2)
                previousFrame = currentFrame
                pendingWrite = toWrite
                pendingDuration = duration - remainingDuration
                pendingDisposalMethod = disposalMethod
            } else {
                pendingDuration += duration
            }
        }
    }

    private fun initAndWriteFrame(
        frame: Image,
        loopCount: Int = this.loopCount,
    ) {
        initAndWriteFrame(
            frame.argb,
            frame.width,
            frame.height,
            pendingDuration,
            pendingDisposalMethod,
            loopCount,
        )
    }

    private fun initAndWriteFrame(
        image: IntArray,
        width: Int,
        height: Int,
        duration: Duration,
        disposalMethod: DisposalMethod,
        loopCount: Int,
    ) {
        init(width, height, loopCount)
        val data = getImageData(image)
        sink.writeGifImage(
            data,
            width,
            height,
            duration.coerceAtLeast(minimumFrameDuration),
            disposalMethod,
        )
        frameCount++
    }

    private fun getImageData(image: IntArray): QuantizedImageData {
        // Build color table
        val rgb = mutableListOf<Byte>()
        val distinctColors = mutableSetOf<Int>()
        var hasTransparent = false
        image.forEach { pixel ->
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
        val distinctColorCountNoTransparent = distinctColors.size
        val distinctColorCount = distinctColorCountNoTransparent + if (hasTransparent) 1 else 0
        val colorCount = distinctColorCount.coerceAtMost(maxColors)
        val colorTableSize = colorCount.roundUpPowerOf2()
            .coerceAtLeast(GIF_MINIMUM_COLOR_TABLE_SIZE)
        val quantizer = if (distinctColorCountNoTransparent > maxColors) {
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
        val imageColorIndices = ByteArray(image.size)
        // First index is reserved for transparent color
        val indexOffset = if (hasTransparent) 1 else 0
        image.forEachIndexed { i, pixel ->
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
            val loopCount = if (this.frameCount > 1) loopCount else -1
            initAndWriteFrame(pendingWrite, loopCount)
        }
        sink.writeGifTrailer()
        sink.close()
    }
}

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
            // Current frame has a transparent pixel where the previous frame had an opaque pixel.
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
    duration: Duration,
    disposalMethod: DisposalMethod,
) {
    val (colorTable, imageColorIndices, colorTableSize, transparentColorIndex) = data
    writeGifGraphicsControlExtension(disposalMethod, duration, transparentColorIndex)
    writeGifImageDescriptor(width, height, colorTableSize)
    writeGifColorTable(colorTable)
    writeGifImageData(imageColorIndices, colorTableSize)
}

internal fun Sink.writeGifGraphicsControlExtension(
    disposalMethod: DisposalMethod,
    delay: Duration,
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
    writeLittleEndianShort(delay.inWholeMilliseconds / 10)
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
