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
    private val transparencyColorTolerance: Double = 0.0,
    private val quantizedTransparencyColorTolerance: Double = 0.0,
    private val optimizeQuantizedTransparency: Boolean = quantizedTransparencyColorTolerance > 0.0,
    private val cropTransparent: Boolean = false,
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
    private var optimizedLastFrame: Boolean = false

    private lateinit var previousQuantizedFrame: Image
    private lateinit var previousQuantizedOriginalFrame: Image
    private var pendingQuantizedData: QuantizedImageData? = null
    private var pendingQuantizedDurationCentiseconds: Int = 0
    private var pendingQuantizedDisposalMethod: DisposalMethod = DisposalMethod.UNSPECIFIED

    private var frameCount: Int = 0

    private fun init(width: Int, height: Int, loopCount: Int) {
        if (initialized) return
        this.width = width
        this.height = height
        sink.writeGifIntro(
            width,
            height,
            loopCount,
            comment,
        )
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
        if (::previousFrame.isInitialized && previousFrame.isSimilar(currentFrame, transparencyColorTolerance)) {
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
            val optimized = optimizeTransparency(
                previousFrame,
                currentFrame,
                transparencyColorTolerance,
                safeTransparent = false,
            )
            if (optimized == null) {
                pendingDisposalMethod = DisposalMethod.RESTORE_TO_BACKGROUND_COLOR
                toWrite = currentFrame
                disposalMethod = DisposalMethod.RESTORE_TO_BACKGROUND_COLOR
            } else {
                toWrite = optimized
                disposalMethod = DisposalMethod.DO_NOT_DISPOSE
            }
            optimizedLastFrame = optimized != null
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
            initAndWriteFrame(pendingWrite1, previousFrame, centiseconds)
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
                initAndWriteFrame(pendingWrite2, previousFrame, minimumFrameDurationCentiseconds)
                previousFrame = currentFrame
                pendingWrite = toWrite
                pendingDuration = newPendingDuration - minimumFrameDuration
                pendingDisposalMethod = disposalMethod
            }
        }
    }

    private fun initAndWriteFrame(
        image: Image,
        originalImage: Image,
        durationCentiseconds: Int,
        loopCount: Int = this.loopCount,
    ) {
        initAndWriteFrame(
            image,
            originalImage,
            durationCentiseconds,
            pendingDisposalMethod,
            loopCount,
        )
    }

    private fun initAndWriteFrame(
        image: Image,
        originalImage: Image,
        durationCentiseconds: Int,
        disposalMethod: DisposalMethod,
        loopCount: Int,
    ) {
        init(image.width, image.height, loopCount)
        val data = getImageData(
            image,
            maxColors,
            quantizer,
            forceTransparency = optimizeQuantizedTransparency,
        )
        if (optimizeQuantizedTransparency) {
            tryWriteOptimizedGifImage(
                data,
                originalImage,
                durationCentiseconds,
                disposalMethod,
            )
        } else {
            writeGifImage(
                data,
                durationCentiseconds,
                disposalMethod,
            )
        }
    }

    private fun tryWriteOptimizedGifImage(
        data: QuantizedImageData,
        originalImage: Image,
        durationCentiseconds: Int,
        disposalMethod: DisposalMethod,
    ) {
        val quantizedImage = data.toImage()
        if (::previousQuantizedFrame.isInitialized
            && previousQuantizedFrame.isSimilar(quantizedImage, quantizedTransparencyColorTolerance)
        ) {
            // Merge similar sequential frames into one
            pendingQuantizedDurationCentiseconds += durationCentiseconds
            return
        }

        // Optimise transparency
        val toWrite: Image
        val actualDisposalMethod: DisposalMethod
        if (!::previousQuantizedFrame.isInitialized) {
            // First frame
            toWrite = quantizedImage
            actualDisposalMethod = disposalMethod
        } else {
            // Subsequent frames
            val optimized = optimizeTransparency(
                previousQuantizedFrame.fillTransparent(previousQuantizedOriginalFrame),
                quantizedImage,
                quantizedTransparencyColorTolerance,
                safeTransparent = optimizedLastFrame,
            )
            if (optimized == null) {
                pendingQuantizedDisposalMethod = disposalMethod
                toWrite = quantizedImage
                actualDisposalMethod = disposalMethod
            } else {
                toWrite = optimized
                actualDisposalMethod = DisposalMethod.DO_NOT_DISPOSE
            }
        }

        /*
         * Write the previous frame if it exists. The
         * previous frame and the current frame have
         * already been established to be different.
         */
        val pendingWrite = pendingQuantizedData
        if (pendingWrite != null) {
            writeGifImage(
                pendingWrite,
                pendingQuantizedDurationCentiseconds,
                actualDisposalMethod,
            )
            pendingQuantizedDurationCentiseconds = 0
            pendingQuantizedData = null
        }

        /*
         * Don't write frame just yet, as the
         * frame's duration will be decided by
         * whether subsequent frames are similar
         * or not.
         */
        previousQuantizedFrame = quantizedImage
        previousQuantizedOriginalFrame = originalImage
        val originalIndices = data.imageColorIndices
        val optimizedArgb = toWrite.argb
        val optimizedColorIndices = ByteArray(originalIndices.size) { i ->
            if (optimizedArgb[i] == 0) {
                0
            } else {
                originalIndices[i]
            }
        }
        pendingQuantizedData = data.copy(imageColorIndices = optimizedColorIndices)
        pendingQuantizedDurationCentiseconds += durationCentiseconds
        pendingQuantizedDisposalMethod = disposalMethod
    }

    private fun writeGifImage(
        data: QuantizedImageData,
        durationCentiseconds: Int,
        disposalMethod: DisposalMethod,
    ) {
        val toWrite = if (cropTransparent) {
            cropTransparentBorder(data)
        } else {
            data
        }
        sink.writeGifImage(
            toWrite,
            durationCentiseconds,
            disposalMethod,
        )
        frameCount++
    }

    override fun close() {
        val pendingWrite = pendingWrite
        if (pendingWrite != null && pendingDuration > Duration.ZERO) {
            if (frameCount < 2) {
                pendingDisposalMethod = DisposalMethod.UNSPECIFIED
            }
            val centiseconds = pendingDuration.roundedUpCentiseconds
                .coerceAtLeast(minimumFrameDurationCentiseconds)
            val loopCount = if (this.frameCount > 1) loopCount else -1
            initAndWriteFrame(
                pendingWrite,
                previousFrame,
                centiseconds,
                loopCount,
            )
        }
        val pendingQuantizedData = pendingQuantizedData
        if (pendingQuantizedData != null) {
            writeGifImage(
                pendingQuantizedData,
                pendingQuantizedDurationCentiseconds,
                pendingQuantizedDisposalMethod,
            )
        }
        sink.writeGifTrailer()
        sink.close()
    }
}
