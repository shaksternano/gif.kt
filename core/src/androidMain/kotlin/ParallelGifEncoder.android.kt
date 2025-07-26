package com.shakster.gifkt

import com.shakster.gifkt.internal.BaseParallelGifEncoder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.future.future
import kotlinx.io.IOException
import kotlinx.io.Sink
import java.util.concurrent.CompletableFuture
import kotlin.coroutines.CoroutineContext
import kotlin.time.Duration
import kotlin.time.toKotlinDuration
import java.time.Duration as JavaDuration

actual class ParallelGifEncoder
@JvmOverloads
actual constructor(
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
    maxConcurrency: Int,
    private val coroutineScope: CoroutineScope,
    ioContext: CoroutineContext,
    onFrameWritten: suspend (
        framesWritten: Int,
        writtenDuration: Duration,
    ) -> Unit,
) : SuspendClosable {

    private val baseEncoder: BaseParallelGifEncoder = BaseParallelGifEncoder(
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
        maxConcurrency,
        coroutineScope,
        ioContext,
        onFrameWritten,
    )

    @Throws(IOException::class)
    actual suspend fun writeFrame(
        argb: IntArray,
        width: Int,
        height: Int,
        duration: Duration,
    ) {
        baseEncoder.writeFrame(argb, width, height, duration)
    }

    @Throws(IOException::class)
    suspend fun writeFrame(
        argb: IntArray,
        width: Int,
        height: Int,
        duration: JavaDuration,
    ) {
        baseEncoder.writeFrame(
            argb,
            width,
            height,
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
