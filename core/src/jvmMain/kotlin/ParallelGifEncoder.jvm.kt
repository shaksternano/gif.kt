package com.shakster.gifkt

import com.shakster.gifkt.internal.BaseParallelGifEncoder
import com.shakster.gifkt.internal.GIF_MAX_COLORS
import com.shakster.gifkt.internal.GIF_MINIMUM_FRAME_DURATION_CENTISECONDS
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.future.future
import kotlinx.io.IOException
import kotlinx.io.Sink
import kotlinx.io.asSink
import kotlinx.io.buffered
import java.awt.image.BufferedImage
import java.io.OutputStream
import java.util.concurrent.CompletableFuture
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.time.Duration
import kotlin.time.toKotlinDuration
import java.time.Duration as JavaDuration

actual class ParallelGifEncoder
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
    maxConcurrency: Int,
    private val coroutineScope: CoroutineScope,
    ioContext: CoroutineContext,
    onFrameWritten: suspend (
        framesWritten: Int,
        writtenDuration: Duration,
    ) -> Unit,
) : SuspendClosable {

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
        maxConcurrency: Int = 2,
        coroutineScope: CoroutineScope = CoroutineScope(EmptyCoroutineContext),
        ioContext: CoroutineContext = EmptyCoroutineContext,
        onFrameWritten: suspend (
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
        maxConcurrency,
        coroutineScope,
        ioContext,
        onFrameWritten,
    )

    private val baseEncoder: BaseParallelGifEncoder = BaseParallelGifEncoder(
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
        maxConcurrency,
        coroutineScope,
        ioContext,
        onFrameWritten,
    )

    @Throws(IOException::class)
    actual suspend fun writeFrame(
        image: IntArray,
        width: Int,
        height: Int,
        duration: Duration,
    ) {
        baseEncoder.writeFrame(image, width, height, duration)
    }

    @Throws(IOException::class)
    suspend fun writeFrame(
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
    suspend fun writeFrame(image: BufferedImage, duration: Duration) {
        baseEncoder.writeFrame(
            image.rgb,
            image.width,
            image.height,
            duration,
        )
    }

    @Throws(IOException::class)
    suspend fun writeFrame(image: BufferedImage, duration: JavaDuration) {
        baseEncoder.writeFrame(
            image.rgb,
            image.width,
            image.height,
            duration.toKotlinDuration(),
        )
    }

    @Throws(IOException::class)
    actual suspend fun writeFrame(frame: ImageFrame) {
        baseEncoder.writeFrame(frame)
    }

    fun writeFrameFuture(
        image: IntArray,
        width: Int,
        height: Int,
        duration: Duration,
    ): CompletableFuture<Void> {
        return coroutineScope.future {
            baseEncoder.writeFrame(image, width, height, duration)
        }.thenAccept { }
    }

    fun writeFrameFuture(
        image: IntArray,
        width: Int,
        height: Int,
        duration: JavaDuration,
    ): CompletableFuture<Void> {
        return coroutineScope.future {
            baseEncoder.writeFrame(
                image,
                width,
                height,
                duration.toKotlinDuration(),
            )
        }.thenAccept { }
    }

    fun writeFrameFuture(image: BufferedImage, duration: Duration): CompletableFuture<Void> {
        return coroutineScope.future {
            baseEncoder.writeFrame(
                image.rgb,
                image.width,
                image.height,
                duration,
            )
        }.thenAccept { }
    }

    fun writeFrameFuture(image: BufferedImage, duration: JavaDuration): CompletableFuture<Void> {
        return coroutineScope.future {
            baseEncoder.writeFrame(
                image.rgb,
                image.width,
                image.height,
                duration.toKotlinDuration(),
            )
        }.thenAccept { }
    }

    fun writeFrameFuture(frame: ImageFrame): CompletableFuture<Void> {
        return coroutineScope.future {
            baseEncoder.writeFrame(frame)
        }.thenAccept { }
    }

    @Throws(IOException::class)
    actual override suspend fun close() {
        baseEncoder.close()
    }

    fun closeFuture(): CompletableFuture<Void> {
        return coroutineScope.future {
            baseEncoder.close()
        }.thenAccept { }
    }
}
