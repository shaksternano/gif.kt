package com.shakster.gifkt

import com.shakster.gifkt.internal.BaseSequentialGifEncoder
import kotlinx.io.IOException
import kotlinx.io.Sink
import kotlin.time.Duration

actual class GifEncoder actual constructor(
    sink: Sink,
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
    onFrameWritten: (
        framesWritten: Int,
        writtenDuration: Duration,
    ) -> Unit,
) : AutoCloseable {

    private val baseEncoder: BaseSequentialGifEncoder = BaseSequentialGifEncoder(
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
}
