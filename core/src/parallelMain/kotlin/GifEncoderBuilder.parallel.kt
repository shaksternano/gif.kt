package com.shakster.gifkt

import kotlin.time.Duration

/**
 * Builds a [ParallelGifEncoder] with the specified parameters.
 *
 * @param onFrameWritten A callback that is invoked after each frame is written,
 * providing the number of frames written and the total duration of all the frames written so far.
 * This can be used to track progress or update a UI.
 *
 * @return The constructed [ParallelGifEncoder].
 */
expect fun GifEncoderBuilder.buildParallel(
    onFrameWritten: suspend (
        framesWritten: Int,
        writtenDuration: Duration,
    ) -> Unit = { _, _ -> },
): ParallelGifEncoder
