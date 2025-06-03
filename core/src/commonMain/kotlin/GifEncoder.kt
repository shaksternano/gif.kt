package com.shakster.gifkt

import com.shakster.gifkt.internal.GIF_MAX_COLORS
import com.shakster.gifkt.internal.GIF_MINIMUM_FRAME_DURATION_CENTISECONDS
import kotlinx.io.Sink
import kotlin.time.Duration

expect class GifEncoder(
    sink: Sink,
    transparencyColorTolerance: Double = 0.0,
    quantizedTransparencyColorTolerance: Double = -1.0,
    loopCount: Int = 0,
    maxColors: Int = GIF_MAX_COLORS,
    quantizer: ColorQuantizer = NeuQuantizer.DEFAULT,
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
}
