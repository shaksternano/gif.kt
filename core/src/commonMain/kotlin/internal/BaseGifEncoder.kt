package com.shakster.gifkt.internal

import com.shakster.gifkt.*
import kotlinx.io.Sink
import kotlin.time.Duration

/*
 * Reference:
 * https://www.matthewflickinger.com/lab/whatsinagif/bits_and_bytes.asp
 */
internal class BaseGifEncoder(
    private val sink: Sink,
    private val colorDifferenceTolerance: Double,
    private val quantizedColorDifferenceTolerance: Double,
    private val loopCount: Int,
    maxColors: Int,
    private val colorQuantizer: ColorQuantizer,
    private val colorSimilarityChecker: ColorSimilarityChecker,
    private val comment: String,
    private val transparentAlphaThreshold: Int,
    private val alphaFill: Int,
    private val cropTransparent: Boolean,
    private val minimumFrameDurationCentiseconds: Int,
) {

    init {
        require(transparentAlphaThreshold in 0..255) {
            "transparentAlphaThreshold must between 0 and 255 inclusive: $transparentAlphaThreshold"
        }

        require(minimumFrameDurationCentiseconds > 0) {
            "minimumFrameDurationCentiseconds must be positive: $minimumFrameDurationCentiseconds"
        }
    }

    private val optimizeTransparency: Boolean = colorDifferenceTolerance >= 0
    val optimizeQuantizedTransparency: Boolean = quantizedColorDifferenceTolerance >= 0
    val maxColors: Int = maxColors.coerceIn(1, GIF_MAX_COLORS)
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

    private var closed: Boolean = false

    /**
     * Writes a frame to the GIF.
     *
     * @return `true` if the frame was written, `false` if it was not written
     * because it was merged with the previous frame due to being similar.
     */
    inline fun writeFrame(
        argb: IntArray,
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
        checkDimensions(argb, width, height)

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
        val currentFrame = Image(argb.copyOf(), width, height)
            .cropOrPad(targetWidth, targetHeight)
            .fillPartialAlpha(alphaFill)

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
                    colorDifferenceTolerance,
                    colorSimilarityChecker,
                    safeTransparent = false,
                )
            } else {
                null
            }
            if (optimized == null) {
                val isEmpty = currentFrame.argb.all {
                    RGB(it).alpha == 0
                }
                if (isEmpty) {
                    // If the frame is fully transparent, we can skip writing it
                    pendingDuration += duration
                    return false
                } else {
                    pendingDisposalMethod = DisposalMethod.RESTORE_TO_BACKGROUND_COLOR
                    toWrite = currentFrame
                }
            } else if (optimized.empty) {
                // Merge similar sequential frames into one
                pendingDuration += duration
                return false
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

    fun quantizeImage(image: Image): QuantizedImageData =
        quantizeImage(
            image,
            maxColors,
            transparentAlphaThreshold,
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
    ): Boolean {
        return if (optimizeQuantizedTransparency) {
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
            true
        }
    }

    private inline fun writeOptimizedGifImage(
        imageData: QuantizedImageData,
        originalImage: Image,
        durationCentiseconds: Int,
        optimizedPreviousFrame: Boolean,
        encodeAndWriteImage: (
            imageData: QuantizedImageData,
            durationCentiseconds: Int,
            disposalMethod: DisposalMethod,
        ) -> Unit,
    ): Boolean {
        // Optimise transparency
        val quantizedImage = imageData.toImage()
        val toWrite = if (!writtenAnyQuantized) {
            // First frame
            quantizedImage
        } else {
            // Subsequent frames
            val optimized = if (optimizedPreviousFrame) {
                optimizeTransparency(
                    previousQuantizedFrame,
                    quantizedImage,
                    quantizedColorDifferenceTolerance,
                    colorSimilarityChecker,
                    safeTransparent = true,
                )
            } else {
                null
            }
            if (optimized == null) {
                pendingQuantizedDisposalMethod = DisposalMethod.RESTORE_TO_BACKGROUND_COLOR
                quantizedImage
            } else if (optimized.empty) {
                // Merge similar sequential frames into one
                pendingQuantizedDurationCentiseconds += durationCentiseconds
                return false
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

        return true
    }

    private inline fun writeGifImage(
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
                var totalFrames = frameCount + 1
                if (pendingQuantizedData != null) {
                    totalFrames++
                }

                val centiseconds: Int
                val actualLoopCount: Int
                if (totalFrames > 1) {
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
            ", colorDifferenceTolerance=$colorDifferenceTolerance" +
            ", quantizedColorDifferenceTolerance=$quantizedColorDifferenceTolerance" +
            ", loopCount=$loopCount" +
            ", colorQuantizer=$colorQuantizer" +
            ", colorSimilarityChecker=$colorSimilarityChecker" +
            ", comment='$comment'" +
            ", transparentAlphaThreshold=$transparentAlphaThreshold" +
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
