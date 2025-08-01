package com.shakster.gifkt.internal

import com.shakster.gifkt.*
import kotlinx.io.Sink
import kotlin.time.Duration

internal class BaseSyncGifEncoder(
    private val sink: Sink,
    colorDifferenceTolerance: Double,
    quantizedColorDifferenceTolerance: Double,
    loopCount: Int,
    maxColors: Int,
    colorQuantizer: ColorQuantizer,
    colorSimilarityChecker: ColorSimilarityChecker,
    comment: String,
    alphaFill: Int,
    cropTransparent: Boolean,
    minimumFrameDurationCentiseconds: Int,
    private val onFrameWritten: (
        framesWritten: Int,
        writtenDuration: Duration,
    ) -> Unit,
) : AutoCloseable {

    private val baseEncoder: BaseGifEncoder = BaseGifEncoder(
        sink,
        colorDifferenceTolerance,
        quantizedColorDifferenceTolerance,
        loopCount,
        maxColors,
        colorQuantizer,
        colorSimilarityChecker,
        comment,
        alphaFill,
        cropTransparent,
        minimumFrameDurationCentiseconds,
    )

    private var framesWritten: Int = 0
    private var writtenDuration: Duration = Duration.ZERO

    fun writeFrame(
        argb: IntArray,
        width: Int,
        height: Int,
        duration: Duration,
    ) {
        val written = baseEncoder.writeFrame(
            argb,
            width,
            height,
            duration,
            ::quantizeAndWriteFrame,
        )

        // Account for frames that have been merged due to similarity.
        if (!written) {
            handleWrittenFrame(Duration.ZERO)
        }
    }

    fun writeFrame(frame: ImageFrame) =
        writeFrame(
            frame.argb,
            frame.width,
            frame.height,
            frame.duration,
        )

    private fun quantizeAndWriteFrame(
        optimizedImage: Image,
        originalImage: Image,
        durationCentiseconds: Int,
        disposalMethod: DisposalMethod,
        optimizedPreviousFrame: Boolean,
    ) {
        val written = baseEncoder.writeOrOptimizeGifImage(
            baseEncoder.quantizeImage(optimizedImage),
            originalImage,
            durationCentiseconds,
            disposalMethod,
            optimizedPreviousFrame,
            ::writeImage,
        )

        // Account for frames that have been merged due to similarity.
        if (!written) {
            handleWrittenFrame(Duration.ZERO)
        }
    }

    private fun writeImage(
        imageData: QuantizedImageData,
        durationCentiseconds: Int,
        disposalMethod: DisposalMethod,
    ) {
        sink.writeQuantizedGifImage(imageData, durationCentiseconds, disposalMethod)
        handleWrittenFrame(durationCentiseconds.centiseconds)
    }

    private fun handleWrittenFrame(duration: Duration) {
        try {
            writtenDuration += duration
            onFrameWritten(++framesWritten, writtenDuration)
        } catch (t: Throwable) {
            throw Exception("Error running onFrameWritten callback", t)
        }
    }

    override fun close() {
        baseEncoder.close(
            ::quantizeAndWriteFrame,
            ::writeImage,
        )
    }
}
