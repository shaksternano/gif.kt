package com.shakster.gifkt

import kotlin.time.Duration

actual fun GifEncoderBuilder.buildParallel(
    onFrameWritten: suspend (
        framesWritten: Int,
        writtenDuration: Duration,
    ) -> Unit,
): ParallelGifEncoder {
    return ParallelGifEncoder(
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
}
