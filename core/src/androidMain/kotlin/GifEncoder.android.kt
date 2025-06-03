package com.shakster.gifkt

import com.shakster.gifkt.internal.BaseSequentialGifEncoder
import com.shakster.gifkt.internal.GIF_MAX_COLORS
import com.shakster.gifkt.internal.GIF_MINIMUM_FRAME_DURATION_CENTISECONDS
import kotlinx.io.Sink
import kotlinx.io.asSink
import kotlinx.io.buffered
import java.io.OutputStream
import kotlin.time.Duration
import kotlin.time.toKotlinDuration
import java.time.Duration as JavaDuration

actual class GifEncoder
@JvmOverloads
actual constructor(
    sink: Sink,
    transparencyColorTolerance: Double,
    quantizedTransparencyColorTolerance: Double,
    loopCount: Int,
    maxColors: Int,
    quantizer: ColorQuantizer,
    comment: String,
    alphaFill: Int,
    cropTransparent: Boolean,
    minimumFrameDurationCentiseconds: Int,
    onFrameWritten: (
        framesWritten: Int,
        writtenDuration: Duration,
    ) -> Unit,
) : AutoCloseable {

    @JvmOverloads
    constructor(
        outputStream: OutputStream,
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
    ) : this(
        outputStream.asSink().buffered(),
        transparencyColorTolerance,
        quantizedTransparencyColorTolerance,
        loopCount,
        maxColors,
        quantizer,
        comment,
        alphaFill,
        cropTransparent,
        minimumFrameDurationCentiseconds,
        onFrameWritten,
    )

    private val baseEncoder: BaseSequentialGifEncoder = BaseSequentialGifEncoder(
        sink,
        transparencyColorTolerance,
        quantizedTransparencyColorTolerance,
        loopCount,
        maxColors,
        quantizer,
        comment,
        alphaFill,
        cropTransparent,
        minimumFrameDurationCentiseconds,
        onFrameWritten,
    )

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

    fun writeFrame(
        image: IntArray,
        width: Int,
        height: Int,
        duration: JavaDuration,
    ) {
        baseEncoder.writeFrame(
            image,
            width,
            height,
            duration.toKotlinDuration(),
        )
    }

    actual fun writeFrame(frame: ImageFrame) {
        baseEncoder.writeFrame(frame)
    }

    actual override fun close() {
        baseEncoder.close()
    }
}
