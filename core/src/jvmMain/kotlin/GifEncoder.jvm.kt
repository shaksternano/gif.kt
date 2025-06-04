package com.shakster.gifkt

import com.shakster.gifkt.internal.BaseSyncGifEncoder
import com.shakster.gifkt.internal.GIF_MAX_COLORS
import com.shakster.gifkt.internal.GIF_MINIMUM_FRAME_DURATION_CENTISECONDS
import kotlinx.io.IOException
import kotlinx.io.Sink
import kotlinx.io.asSink
import kotlinx.io.buffered
import java.awt.image.BufferedImage
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

    @JvmOverloads
    constructor(
        outputStream: OutputStream,
        transparencyColorTolerance: Double = 0.0,
        quantizedTransparencyColorTolerance: Double = -1.0,
        loopCount: Int = 0,
        maxColors: Int = GIF_MAX_COLORS,
        quantizer: ColorQuantizer = NeuQuantizer.DEFAULT,
        colorDistanceCalculator: ColorDistanceCalculator = CieLabDistanceCalculator,
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
        colorDistanceCalculator,
        comment,
        alphaFill,
        cropTransparent,
        minimumFrameDurationCentiseconds,
        onFrameWritten,
    )

    private val baseEncoder: BaseSyncGifEncoder = BaseSyncGifEncoder(
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

    @Throws(IOException::class)
    fun writeFrame(image: BufferedImage, duration: Duration) {
        baseEncoder.writeFrame(
            image.rgb,
            image.width,
            image.height,
            duration,
        )
    }

    @Throws(IOException::class)
    fun writeFrame(image: BufferedImage, duration: JavaDuration) {
        baseEncoder.writeFrame(
            image.rgb,
            image.width,
            image.height,
            duration.toKotlinDuration(),
        )
    }

    @Throws(IOException::class)
    actual fun writeFrame(frame: ImageFrame) {
        baseEncoder.writeFrame(frame)
    }

    @Throws(IOException::class)
    actual override fun close() {
        baseEncoder.close()
    }
}
