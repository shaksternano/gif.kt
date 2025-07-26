package com.shakster.gifkt

import kotlinx.io.Sink
import kotlin.time.Duration

const val GIF_MAX_COLORS: Int = 256
const val GIF_MINIMUM_FRAME_DURATION_CENTISECONDS: Int = 2

expect class GifEncoder(
    sink: Sink,
    colorDifferenceTolerance: Double = 0.0,
    quantizedColorDifferenceTolerance: Double = -1.0,
    loopCount: Int = 0,
    maxColors: Int = GIF_MAX_COLORS,
    colorQuantizer: ColorQuantizer = ColorQuantizer.DEFAULT,
    colorSimilarityChecker: ColorSimilarityChecker = ColorSimilarityChecker.DEFAULT,
    comment: String = "",
    alphaFill: Int = -1,
    cropTransparent: Boolean = true,
    minimumFrameDurationCentiseconds: Int = GIF_MINIMUM_FRAME_DURATION_CENTISECONDS,
    onFrameWritten: (
        framesWritten: Int,
        writtenDuration: Duration,
    ) -> Unit = { _, _ -> },
) : AutoCloseable {

    fun writeFrame(
        image: IntArray,
        width: Int,
        height: Int,
        duration: Duration,
    )

    fun writeFrame(frame: ImageFrame)

    override fun close()

    companion object {
        fun builder(sink: Sink): GifEncoderBuilder
    }
}
