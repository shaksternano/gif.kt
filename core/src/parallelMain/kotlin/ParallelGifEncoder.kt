package com.shakster.gifkt

import kotlinx.coroutines.CoroutineScope
import kotlinx.io.Sink
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.time.Duration

expect class ParallelGifEncoder(
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
    maxConcurrency: Int = 2,
    coroutineScope: CoroutineScope = CoroutineScope(EmptyCoroutineContext),
    ioContext: CoroutineContext = EmptyCoroutineContext,
    onFrameWritten: suspend (
        framesWritten: Int,
        writtenDuration: Duration,
    ) -> Unit = { _, _ -> },
) : SuspendClosable {

    suspend fun writeFrame(
        argb: IntArray,
        width: Int,
        height: Int,
        duration: Duration,
    )

    suspend fun writeFrame(frame: ImageFrame)

    override suspend fun close()
}
