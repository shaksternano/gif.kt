package com.shakster.gifkt

import com.shakster.gifkt.internal.BaseSyncGifEncoder
import kotlinx.io.IOException
import kotlinx.io.Sink
import kotlin.time.Duration

actual class GifEncoder actual constructor(
    sink: Sink,
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
    onFrameWritten: (
        framesWritten: Int,
        writtenDuration: Duration,
    ) -> Unit,
) : AutoCloseable {

    private val baseEncoder: BaseSyncGifEncoder = BaseSyncGifEncoder(
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
        onFrameWritten,
    )

    @Throws(IOException::class)
    actual fun writeFrame(
        image: IntArray,
        width: Int,
        height: Int,
        duration: Duration,
    ) {
        baseEncoder.writeFrame(
            image,
            width,
            height,
            duration,
        )
    }

    @Throws(IOException::class)
    actual fun writeFrame(frame: ImageFrame) {
        baseEncoder.writeFrame(frame)
    }

    actual override fun close() {
        baseEncoder.close()
    }

    actual companion object {
        actual fun builder(sink: Sink): GifEncoderBuilder {
            return GifEncoderBuilder(sink)
        }
    }
}
