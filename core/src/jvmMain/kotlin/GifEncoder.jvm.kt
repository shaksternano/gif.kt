package com.shakster.gifkt

import com.shakster.gifkt.internal.BaseSyncGifEncoder
import kotlinx.io.IOException
import kotlinx.io.Sink
import kotlinx.io.asSink
import kotlinx.io.buffered
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import java.awt.image.BufferedImage
import java.io.File
import java.io.OutputStream
import kotlin.io.path.outputStream
import kotlin.time.Duration
import kotlin.time.toKotlinDuration
import java.nio.file.Path as JavaPath
import java.time.Duration as JavaDuration

actual class GifEncoder
@JvmOverloads
actual constructor(
    sink: Sink,
    transparencyColorTolerance: Double,
    quantizedTransparencyColorTolerance: Double,
    loopCount: Int,
    maxColors: Int,
    colorQuantizer: ColorQuantizer,
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

    private val baseEncoder: BaseSyncGifEncoder = BaseSyncGifEncoder(
        sink,
        transparencyColorTolerance,
        quantizedTransparencyColorTolerance,
        loopCount,
        maxColors,
        colorQuantizer,
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

    actual companion object {
        @JvmStatic
        actual fun builder(sink: Sink): GifEncoderBuilder {
            return GifEncoderBuilder(sink)
        }

        @JvmStatic
        fun builder(outputStream: OutputStream): GifEncoderBuilder {
            return GifEncoderBuilder(outputStream.asSink().buffered())
        }

        @JvmStatic
        fun builder(path: Path): GifEncoderBuilder {
            return GifEncoderBuilder(SystemFileSystem.sink(path).buffered())
        }

        @JvmStatic
        fun builder(path: JavaPath): GifEncoderBuilder {
            return GifEncoderBuilder(path.outputStream().asSink().buffered())
        }

        @JvmStatic
        fun builder(file: File): GifEncoderBuilder {
            return GifEncoderBuilder(file.outputStream().asSink().buffered())
        }
    }
}
