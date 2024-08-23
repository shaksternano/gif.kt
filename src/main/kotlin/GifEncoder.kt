package io.github.shaksternano.gifcodec

import kotlinx.io.Sink
import kotlin.time.Duration

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
