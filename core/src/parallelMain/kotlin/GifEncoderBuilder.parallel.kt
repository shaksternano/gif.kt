package com.shakster.gifkt

import kotlin.time.Duration

expect fun GifEncoderBuilder.buildParallel(
    onFrameWritten: suspend (
        framesWritten: Int,
        writtenDuration: Duration,
    ) -> Unit = { _, _ -> },
): ParallelGifEncoder
