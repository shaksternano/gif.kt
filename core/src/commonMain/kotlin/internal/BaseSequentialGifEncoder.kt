package com.shakster.gifkt.internal

import com.shakster.gifkt.ColorDistanceCalculator
import com.shakster.gifkt.ColorQuantizer
import com.shakster.gifkt.ImageFrame
import kotlinx.io.Sink
import kotlin.time.Duration

class BaseSequentialGifEncoder(
    private val sink: Sink,
    transparencyColorTolerance: Double,
    quantizedTransparencyColorTolerance: Double,
    loopCount: Int,
    maxColors: Int,
    quantizer: ColorQuantizer,
    colorDistanceCalculator: ColorDistanceCalculator,
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
        transparencyColorTolerance,
        quantizedTransparencyColorTolerance,
        loopCount,
        maxColors,
        quantizer,
        colorDistanceCalculator,
        comment,
        alphaFill,
        cropTransparent,
        minimumFrameDurationCentiseconds,
    )

    private var framesWritten: Int = 0
    private var writtenDuration: Duration = Duration.ZERO

    fun writeFrame(
        image: IntArray,
        width: Int,
        height: Int,
        duration: Duration,
    ) {
        val written = baseEncoder.writeFrame(
            image,
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
        baseEncoder.writeOrOptimizeGifImage(
            baseEncoder.getImageData(optimizedImage),
            originalImage,
            durationCentiseconds,
            disposalMethod,
            optimizedPreviousFrame,
            ::writeImage,
        )
    }

    private fun writeImage(
        imageData: QuantizedImageData,
        durationCentiseconds: Int,
        disposalMethod: DisposalMethod,
    ) {
        sink.writeGifImage(imageData, durationCentiseconds, disposalMethod)
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
