package io.github.shaksternano.gifcodec

import kotlinx.io.Sink
import kotlin.time.Duration

/*
 * Reference:
 * https://www.matthewflickinger.com/lab/whatsinagif/bits_and_bytes.asp
 */
internal class BaseGifEncoder(
    private val sink: Sink,
    private val loopCount: Int,
    maxColors: Int,
    private val transparencyColorTolerance: Double,
    private val optimizeTransparency: Boolean,
    private val quantizedTransparencyColorTolerance: Double,
    private val optimizeQuantizedTransparency: Boolean,
    private val cropTransparent: Boolean,
    private val alphaFill: Int,
    private val comment: String ,
    private val minimumFrameDurationCentiseconds: Int,
    private val quantizer: ColorQuantizer,
) {

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
    private val writtenAny: Boolean
        get() = ::previousFrame.isInitialized

    private var optimizedPreviousFrame: Boolean = false

    private lateinit var previousQuantizedFrame: Image
    private var pendingQuantizedData: QuantizedImageData? = null
    private var pendingQuantizedDurationCentiseconds: Int = 0
    private var pendingQuantizedDisposalMethod: DisposalMethod = DisposalMethod.UNSPECIFIED
    private val writtenAnyQuantized: Boolean
        get() = ::previousQuantizedFrame.isInitialized

    private var frameCount: Int = 0
    private var nextCrop: Rectangle? = null

    inline fun writeFrame(
        image: IntArray,
        width: Int,
        height: Int,
        duration: Duration,
        quantizeAndWriteFrame: (Image, Image, Int, DisposalMethod, Boolean) -> Unit,
        wrapIo: (() -> Unit) -> Unit = { it() },
    ): Boolean {
        /*
         * Handle the minimum frame duration.
         */
        val newPendingDuration = pendingDuration + duration
        if (writtenAny && newPendingDuration <= minimumFrameDuration) {
            pendingDuration = newPendingDuration
            return false
        }

        /*
         * Make sure all frames have the same dimensions
         * by cropping or padding with transparent pixels,
         * and remove any partial alpha, as GIFs do not
         * support partial transparency.
         */
        val targetWidth = this.width ?: width
        val targetHeight = this.height ?: height
        val currentFrame = Image(image, width, height)
            .cropOrPad(targetWidth, targetHeight)
            .fillPartialAlpha(alphaFill)
        if (optimizeTransparency
            && writtenAny
            && previousFrame.isSimilar(currentFrame, transparencyColorTolerance)
        ) {
            // Merge similar sequential frames into one
            pendingDuration += duration
            return false
        }

        // Optimise transparency
        val toWrite: Image
        val optimizedTransparency: Boolean
        if (!writtenAny) {
            // First frame
            toWrite = currentFrame
            optimizedTransparency = false
        } else {
            // Subsequent frames
            val optimized = if (optimizeTransparency) {
                optimizeTransparency(
                    previousFrame,
                    currentFrame,
                    transparencyColorTolerance,
                    safeTransparent = false,
                )
            } else {
                null
            }
            if (optimized == null) {
                pendingDisposalMethod = DisposalMethod.RESTORE_TO_BACKGROUND_COLOR
                toWrite = currentFrame
            } else {
                pendingDisposalMethod = DisposalMethod.DO_NOT_DISPOSE
                toWrite = optimized
            }
            optimizedTransparency = optimized != null
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
            initAndWriteFrame(
                pendingWrite1,
                previousFrame,
                centiseconds,
                pendingDisposalMethod,
                loopCount,
                quantizeAndWriteFrame,
                wrapIo,
            )
            // Might end up being negative
            pendingDuration -= centiseconds.centiseconds
            pendingWrite = null
        }

        /*
         * Don't write frame just yet, as the
         * frame's duration will be decided by
         * whether subsequent frames are similar
         * or not.
         */
        val pendingWrite2 = pendingWrite
        if (pendingWrite2 == null) {
            pendingDuration += duration
        } else {
            initAndWriteFrame(
                pendingWrite2,
                previousFrame,
                minimumFrameDurationCentiseconds,
                pendingDisposalMethod,
                loopCount,
                quantizeAndWriteFrame,
                wrapIo,
            )
            pendingDuration = newPendingDuration - minimumFrameDuration
        }
        previousFrame = currentFrame
        pendingWrite = toWrite
        optimizedPreviousFrame = optimizedTransparency
        return true
    }

    private inline fun init(
        width: Int,
        height: Int,
        loopCount: Int,
        wrapIo: (() -> Unit) -> Unit,
    ) {
        if (initialized) return
        this.width = width
        this.height = height
        wrapIo {
            sink.writeGifIntro(
                width,
                height,
                loopCount,
                comment,
            )
        }
        initialized = true
    }

    private inline fun initAndWriteFrame(
        image: Image,
        originalImage: Image,
        durationCentiseconds: Int,
        disposalMethod: DisposalMethod,
        loopCount: Int,
        quantizeAndWriteFrame: (Image, Image, Int, DisposalMethod, Boolean) -> Unit,
        wrapIo: (() -> Unit) -> Unit,
    ) {
        init(image.width, image.height, loopCount, wrapIo)
        quantizeAndWriteFrame(image, originalImage, durationCentiseconds, disposalMethod, optimizedPreviousFrame)
    }

    fun getImageData(image: Image): QuantizedImageData =
        getImageData(
            image,
            maxColors,
            quantizer,
            optimizeQuantizedTransparency,
        )

    inline fun writeOrOptimizeGifImage(
        imageData: QuantizedImageData,
        originalImage: Image,
        durationCentiseconds: Int,
        disposalMethod: DisposalMethod,
        optimizedPreviousFrame: Boolean,
        encodeAndWriteImage: (QuantizedImageData, Int, DisposalMethod) -> Unit,
    ) {
        if (optimizeQuantizedTransparency) {
            writeOptimizedGifImage(
                imageData,
                originalImage,
                durationCentiseconds,
                optimizedPreviousFrame,
                encodeAndWriteImage,
            )
        } else {
            writeGifImage(
                imageData,
                durationCentiseconds,
                disposalMethod,
                encodeAndWriteImage,
            )
        }
    }

    private inline fun writeOptimizedGifImage(
        imageData: QuantizedImageData,
        originalImage: Image,
        durationCentiseconds: Int,
        optimizedPreviousFrame: Boolean,
        encodeAndWriteImage: (QuantizedImageData, Int, DisposalMethod) -> Unit,
    ) {
        val quantizedImage = imageData.toImage()
        if (writtenAnyQuantized
            && previousQuantizedFrame.isSimilar(quantizedImage, quantizedTransparencyColorTolerance)
        ) {
            // Merge similar sequential frames into one
            pendingQuantizedDurationCentiseconds += durationCentiseconds
            return
        }

        // Optimise transparency
        val toWrite = if (!writtenAnyQuantized) {
            // First frame
            quantizedImage
        } else {
            // Subsequent frames
            val optimized = if (optimizedPreviousFrame) {
                optimizeTransparency(
                    previousQuantizedFrame,
                    quantizedImage,
                    quantizedTransparencyColorTolerance,
                    safeTransparent = true,
                )
            } else {
                null
            }
            if (optimized == null) {
                pendingQuantizedDisposalMethod = DisposalMethod.RESTORE_TO_BACKGROUND_COLOR
                quantizedImage
            } else {
                pendingQuantizedDisposalMethod = DisposalMethod.DO_NOT_DISPOSE
                optimized
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
                pendingQuantizedDisposalMethod,
                encodeAndWriteImage,
            )
            pendingQuantizedDurationCentiseconds = 0
        }

        /*
         * Don't write frame just yet, as the
         * frame's duration will be decided by
         * whether subsequent frames are similar
         * or not.
         */
        previousQuantizedFrame = quantizedImage.fillTransparent(originalImage)
        val originalIndices = imageData.imageColorIndices
        val optimizedArgb = toWrite.argb
        val optimizedColorIndices = ByteArray(originalIndices.size) { i ->
            if (optimizedArgb[i] == 0) {
                0
            } else {
                originalIndices[i]
            }
        }
        pendingQuantizedData = imageData.copy(imageColorIndices = optimizedColorIndices)
        pendingQuantizedDurationCentiseconds += durationCentiseconds
    }

    private inline fun writeGifImage(
        imageData: QuantizedImageData,
        durationCentiseconds: Int,
        disposalMethod: DisposalMethod,
        encodeAndWriteImage: (QuantizedImageData, Int, DisposalMethod) -> Unit,
    ) {
        val nextCrop = nextCrop
        val toWrite = if (nextCrop != null) {
            val (x, y, width, height) = nextCrop union imageData.opaqueArea()
            this.nextCrop = null
            imageData.crop(x, y, width, height)
        } else if (cropTransparent && frameCount > 0) {
            imageData.cropTransparentBorder()
        } else {
            imageData
        }
        encodeAndWriteImage(
            toWrite,
            durationCentiseconds,
            disposalMethod,
        )
        frameCount++
        if (disposalMethod == DisposalMethod.DO_NOT_DISPOSE) {
            this.nextCrop = toWrite.bounds
        }
    }

    inline fun close(
        quantizeAndWriteFrame: (Image, Image, Int, DisposalMethod, Boolean) -> Unit,
        encodeAndWriteImage: (QuantizedImageData, Int, DisposalMethod) -> Unit,
        afterFinalWrite: () -> Unit = {},
        afterFinalQuantizedWrite: () -> Unit = {},
        wrapIo: (() -> Unit) -> Unit = { it() },
    ) {
        val pendingWrite = pendingWrite
        if (pendingWrite != null && pendingDuration > Duration.ZERO) {
            val centiseconds: Int
            val actualLoopCount: Int
            if (frameCount > 1) {
                centiseconds = pendingDuration.roundedUpCentiseconds
                    .coerceAtLeast(minimumFrameDurationCentiseconds)
                actualLoopCount = loopCount
            } else {
                centiseconds = 0
                actualLoopCount = -1
            }
            initAndWriteFrame(
                pendingWrite,
                previousFrame,
                centiseconds,
                pendingDisposalMethod,
                actualLoopCount,
                quantizeAndWriteFrame,
                wrapIo,
            )
        }
        afterFinalWrite()
        val pendingQuantizedData = pendingQuantizedData
        if (pendingQuantizedData != null) {
            writeGifImage(
                pendingQuantizedData,
                pendingQuantizedDurationCentiseconds,
                pendingQuantizedDisposalMethod,
                encodeAndWriteImage,
            )
        }
        afterFinalQuantizedWrite()
        wrapIo {
            sink.writeGifTrailer()
            sink.close()
        }
    }
}
