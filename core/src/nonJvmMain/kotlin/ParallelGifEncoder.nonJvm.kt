package com.shakster.gifkt

import com.shakster.gifkt.internal.BaseParallelGifEncoder
import kotlinx.coroutines.CoroutineScope
import kotlinx.io.IOException
import kotlinx.io.Sink
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.Duration

actual class ParallelGifEncoder actual constructor(
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
    maxConcurrency: Int,
    coroutineScope: CoroutineScope,
    ioContext: CoroutineContext,
    onFrameWritten: suspend (
        framesWritten: Int,
        writtenDuration: Duration,
    ) -> Unit,
) : SuspendClosable {

    private val baseEncoder: BaseParallelGifEncoder = BaseParallelGifEncoder(
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
        maxConcurrency,
        coroutineScope,
        ioContext,
        onFrameWritten,
    )

    @Throws(CancellationException::class, IOException::class)
    actual suspend fun writeFrame(
        image: IntArray,
        width: Int,
        height: Int,
        duration: Duration,
    ) {
        baseEncoder.writeFrame(image, width, height, duration)
    }

    @Throws(CancellationException::class, IOException::class)
    actual suspend fun writeFrame(frame: ImageFrame) {
        baseEncoder.writeFrame(frame)
    }

    actual override suspend fun close() {
        baseEncoder.close()
    }
}
