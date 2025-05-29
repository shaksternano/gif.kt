package com.shakster.gifkt

import com.shakster.gifkt.internal.*
import kotlinx.io.Sink
import kotlin.time.Duration

class GifEncoder(
    private val sink: Sink,
    loopCount: Int = 0,
    maxColors: Int = GIF_MAX_COLORS,
    transparencyColorTolerance: Double = 0.0,
    optimizeTransparency: Boolean = true,
    quantizedTransparencyColorTolerance: Double = 0.0,
    optimizeQuantizedTransparency: Boolean = quantizedTransparencyColorTolerance > 0.0,
    cropTransparent: Boolean = true,
    alphaFill: Int = -1,
    comment: String = "",
    minimumFrameDurationCentiseconds: Int = GIF_MINIMUM_FRAME_DURATION_CENTISECONDS,
    quantizer: ColorQuantizer = NeuQuantizer.DEFAULT,
    private val onFrameWritten: (index: Int) -> Unit = {},
) : AutoCloseable {

    private val baseEncoder: BaseGifEncoder = BaseGifEncoder(
        sink,
        loopCount,
        maxColors,
        transparencyColorTolerance,
        optimizeTransparency,
        quantizedTransparencyColorTolerance,
        optimizeQuantizedTransparency,
        cropTransparent,
        alphaFill,
        comment,
        minimumFrameDurationCentiseconds,
        quantizer,
    )

    private var writtenFrameIndex: Int = 0

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
            handleWrittenFrame()
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
        handleWrittenFrame()
    }

    private fun handleWrittenFrame() {
        try {
            onFrameWritten(writtenFrameIndex++)
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
