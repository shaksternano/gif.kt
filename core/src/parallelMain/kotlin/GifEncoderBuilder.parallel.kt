@file:JvmName("GifEncoderBuilderParallel")

package com.shakster.gifkt

import kotlinx.coroutines.CoroutineScope
import kotlin.coroutines.CoroutineContext
import kotlin.jvm.JvmName
import kotlin.time.Duration

/**
 * The maximum number of frames that can be processed concurrently at the same time.
 * Used when creating a [ParallelGifEncoder].
 */
expect var GifEncoderBuilder.maxConcurrency: Int

/**
 * The [CoroutineScope] in which the concurrent encoding operations will run.
 * Used when creating a [ParallelGifEncoder].
 */
expect var GifEncoderBuilder.coroutineScope: CoroutineScope

/**
 * The [CoroutineContext] to use for writing to the sink.
 * Used when creating a [ParallelGifEncoder].
 */
expect var GifEncoderBuilder.ioContext: CoroutineContext

/**
 * Builds a [ParallelGifEncoder] with the specified parameters.
 *
 * @param onFrameWritten A callback that is invoked after each frame is written,
 * providing the number of frames written and the total duration of all the frames written so far.
 * This can be used to track progress or update a UI.
 *
 * @return The constructed [ParallelGifEncoder].
 *
 * @throws IllegalArgumentException If any of the builder parameters are invalid.
 */
expect fun GifEncoderBuilder.buildParallel(
    onFrameWritten: suspend (
        framesWritten: Int,
        writtenDuration: Duration,
    ) -> Unit = { _, _ -> },
): ParallelGifEncoder
