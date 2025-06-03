package com.shakster.gifkt

import com.shakster.gifkt.internal.*
import kotlinx.io.Sink
import kotlin.time.Duration

class GifEncoder(
    private val sink: Sink,
    loopCount: Int = 0,
    maxColors: Int = GIF_MAX_COLORS,
    transparencyColorTolerance: Double = 0.0,
    quantizedTransparencyColorTolerance: Double = -1.0,
    cropTransparent: Boolean = true,
    alphaFill: Int = -1,
    comment: String = "",
    minimumFrameDurationCentiseconds: Int = GIF_MINIMUM_FRAME_DURATION_CENTISECONDS,
    quantizer: ColorQuantizer = NeuQuantizer.DEFAULT,
    private val onFrameWritten: (
        framesWritten: Int,
        writtenDuration: Duration,
    ) -> Unit = { _, _ -> },
) : AutoCloseable {

    private val baseEncoder: BaseGifEncoder = BaseGifEncoder(
        sink,
        loopCount,
        maxColors,
        transparencyColorTolerance,
        quantizedTransparencyColorTolerance,
        cropTransparent,
        alphaFill,
        comment,
        minimumFrameDurationCentiseconds,
        quantizer,
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
