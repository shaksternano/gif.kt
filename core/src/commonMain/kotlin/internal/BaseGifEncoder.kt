package com.shakster.gifkt.internal

import com.shakster.gifkt.ColorDistanceCalculator
import com.shakster.gifkt.ColorQuantizer
import kotlinx.io.Sink
import kotlin.time.Duration

/*
 * Reference:
 * https://www.matthewflickinger.com/lab/whatsinagif/bits_and_bytes.asp
 */
class BaseGifEncoder(
    val sink: Sink,
    val transparencyColorTolerance: Double,
    val quantizedTransparencyColorTolerance: Double,
    val loopCount: Int,
    maxColors: Int,
    val colorQuantizer: ColorQuantizer,
    val colorDistanceCalculator: ColorDistanceCalculator,
    val comment: String,
    val alphaFill: Int,
    val cropTransparent: Boolean,
    val minimumFrameDurationCentiseconds: Int,
) {

    init {
        require(minimumFrameDurationCentiseconds > 0) {
            "minimumFrameDurationCentiseconds must be positive: $minimumFrameDurationCentiseconds"
        }
    }

    val optimizeTransparency: Boolean = transparencyColorTolerance >= 0
    val optimizeQuantizedTransparency: Boolean = quantizedTransparencyColorTolerance >= 0
    val maxColors: Int = maxColors.coerceIn(1, GIF_MAX_COLORS)
    val minimumFrameDuration: Duration = minimumFrameDurationCentiseconds.centiseconds

    @PublishedApi
    internal var initialized: Boolean = false

    @PublishedApi
    internal var width: Int? = null

    @PublishedApi
    internal var height: Int? = null

    @PublishedApi
    internal lateinit var previousFrame: Image

    @PublishedApi
    internal var pendingWrite: Image? = null

    @PublishedApi
    internal var pendingDuration: Duration = Duration.ZERO

    @PublishedApi
    internal var pendingDisposalMethod: DisposalMethod = DisposalMethod.UNSPECIFIED

    @PublishedApi
    internal val writtenAny: Boolean
        get() = ::previousFrame.isInitialized

    @PublishedApi
    internal var optimizedPreviousFrame: Boolean = false

    @PublishedApi
    internal lateinit var previousQuantizedFrame: Image

    @PublishedApi
    internal var pendingQuantizedData: QuantizedImageData? = null

    @PublishedApi
    internal var pendingQuantizedDurationCentiseconds: Int = 0

    @PublishedApi
    internal var pendingQuantizedDisposalMethod: DisposalMethod = DisposalMethod.UNSPECIFIED

    @PublishedApi
    internal val writtenAnyQuantized: Boolean
        get() = ::previousQuantizedFrame.isInitialized

    @PublishedApi
    internal var frameCount: Int = 0

    @PublishedApi
    internal var nextCrop: Rectangle? = null

    @PublishedApi
    internal var closed: Boolean = false

    /**
     * Writes a frame to the GIF.
     *
     * @return `true` if the frame was written, `false` if it was not written
     * because it was merged with the previous frame due to being similar.
     */
    inline fun writeFrame(
        image: IntArray,
        width: Int,
        height: Int,
        duration: Duration,
        quantizeAndWriteFrame: (
            optimizedImage: Image,
            originalImage: Image,
            durationCentiseconds: Int,
            disposalMethod: DisposalMethod,
            optimizedPreviousFrame: Boolean,
        ) -> Unit,
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
            && previousFrame.isSimilar(
                currentFrame,
                transparencyColorTolerance,
                colorDistanceCalculator,
            )
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
                    colorDistanceCalculator,
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

    @PublishedApi
    internal inline fun init(
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

    @PublishedApi
    internal inline fun initAndWriteFrame(
        image: Image,
        originalImage: Image,
        durationCentiseconds: Int,
        disposalMethod: DisposalMethod,
        loopCount: Int,
        quantizeAndWriteFrame: (
            optimizedImage: Image,
            originalImage: Image,
            durationCentiseconds: Int,
            disposalMethod: DisposalMethod,
            optimizedPreviousFrame: Boolean,
        ) -> Unit,
        wrapIo: (() -> Unit) -> Unit,
    ) {
        init(image.width, image.height, loopCount, wrapIo)
        quantizeAndWriteFrame(image, originalImage, durationCentiseconds, disposalMethod, optimizedPreviousFrame)
    }

    fun getImageData(image: Image): QuantizedImageData =
        getImageData(
            image,
            maxColors,
            colorQuantizer,
            optimizeQuantizedTransparency,
        )

    inline fun writeOrOptimizeGifImage(
        imageData: QuantizedImageData,
        originalImage: Image,
        durationCentiseconds: Int,
        disposalMethod: DisposalMethod,
        optimizedPreviousFrame: Boolean,
        encodeAndWriteImage: (
            imageData: QuantizedImageData,
            durationCentiseconds: Int,
            disposalMethod: DisposalMethod,
        ) -> Unit,
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

    @PublishedApi
    internal inline fun writeOptimizedGifImage(
        imageData: QuantizedImageData,
        originalImage: Image,
        durationCentiseconds: Int,
        optimizedPreviousFrame: Boolean,
        encodeAndWriteImage: (
            imageData: QuantizedImageData,
            durationCentiseconds: Int,
            disposalMethod: DisposalMethod,
        ) -> Unit,
    ) {
        val quantizedImage = imageData.toImage()
        if (writtenAnyQuantized
            && previousQuantizedFrame.isSimilar(
                quantizedImage,
                quantizedTransparencyColorTolerance,
                colorDistanceCalculator,
            )
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
                    colorDistanceCalculator,
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

    @PublishedApi
    internal inline fun writeGifImage(
        imageData: QuantizedImageData,
        durationCentiseconds: Int,
        disposalMethod: DisposalMethod,
        encodeAndWriteImage: (
            imageData: QuantizedImageData,
            durationCentiseconds: Int,
            disposalMethod: DisposalMethod,
        ) -> Unit,
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
        quantizeAndWriteFrame: (
            optimizedImage: Image,
            originalImage: Image,
            durationCentiseconds: Int,
            disposalMethod: DisposalMethod,
            optimizedPreviousFrame: Boolean,
        ) -> Unit,
        encodeAndWriteImage: (
            imageData: QuantizedImageData,
            durationCentiseconds: Int,
            disposalMethod: DisposalMethod,
        ) -> Unit,
        afterFinalWrite: () -> Unit = {},
        wrapIo: (() -> Unit) -> Unit = { it() },
    ) {
        if (closed) return
        closed = true
        var closeThrowable: Throwable? = null
        try {
            val pendingWrite = pendingWrite
            if (pendingWrite != null && (frameCount == 0 || pendingDuration > Duration.ZERO)) {
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
            val pendingQuantizedData = pendingQuantizedData
            if (pendingQuantizedData != null) {
                writeGifImage(
                    pendingQuantizedData,
                    pendingQuantizedDurationCentiseconds,
                    pendingQuantizedDisposalMethod,
                    encodeAndWriteImage,
                )
            }
            afterFinalWrite()
            wrapIo {
                sink.writeGifTrailer()
            }
        } catch (t: Throwable) {
            closeThrowable = t
            throw t
        } finally {
            try {
                wrapIo {
                    sink.close()
                }
            } catch (t: Throwable) {
                if (closeThrowable == null) {
                    throw t
                } else {
                    closeThrowable.addSuppressed(t)
                }
            }
        }
    }

    override fun toString(): String {
        return "BaseGifEncoder(" +
            "sink=$sink" +
            ", transparencyColorTolerance=$transparencyColorTolerance" +
            ", quantizedTransparencyColorTolerance=$quantizedTransparencyColorTolerance" +
            ", loopCount=$loopCount" +
            ", colorQuantizer=$colorQuantizer" +
            ", colorDistanceCalculator=$colorDistanceCalculator" +
            ", comment='$comment'" +
            ", alphaFill=$alphaFill" +
            ", cropTransparent=$cropTransparent" +
            ", minimumFrameDurationCentiseconds=$minimumFrameDurationCentiseconds" +
            ", optimizeTransparency=$optimizeTransparency" +
            ", optimizeQuantizedTransparency=$optimizeQuantizedTransparency" +
            ", maxColors=$maxColors" +
            ", minimumFrameDuration=$minimumFrameDuration" +
            ", initialized=$initialized" +
            ", width=$width" +
            ", height=$height" +
            ", previousFrame=$previousFrame" +
            ", pendingWrite=$pendingWrite" +
            ", pendingDuration=$pendingDuration" +
            ", pendingDisposalMethod=$pendingDisposalMethod" +
            ", writtenAny=$writtenAny" +
            ", optimizedPreviousFrame=$optimizedPreviousFrame" +
            ", previousQuantizedFrame=$previousQuantizedFrame" +
            ", pendingQuantizedData=$pendingQuantizedData" +
            ", pendingQuantizedDurationCentiseconds=$pendingQuantizedDurationCentiseconds" +
            ", pendingQuantizedDisposalMethod=$pendingQuantizedDisposalMethod" +
            ", writtenAnyQuantized=$writtenAnyQuantized" +
            ", frameCount=$frameCount" +
            ", nextCrop=$nextCrop" +
            ", closed=$closed" +
            ")"
    }
}
