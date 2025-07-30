package com.shakster.gifkt

import kotlinx.coroutines.CoroutineScope
import kotlinx.io.Sink
import kotlin.coroutines.CoroutineContext
import kotlin.time.Duration

/**
 * A builder for creating [GifEncoder]s.
 *
 * @param sink The [Sink] to write the GIF data to.
 */
expect class GifEncoderBuilder(
    sink: Sink,
) {

    /**
     * The tolerance for color difference used by [colorSimilarityChecker]
     * when performing transparency optimization.
     *
     * Set to -1 to disable transparency optimization.
     */
    var colorDifferenceTolerance: Double

    /**
     * The tolerance for color difference used by [colorSimilarityChecker]
     * when performing transparency optimization after quantization.
     *
     * Set to -1 to disable post-quantization transparency optimization.
     */
    var quantizedColorDifferenceTolerance: Double

    /**
     * The number of times the GIF should loop.
     *
     * Set to 0 for infinite looping.
     *
     * Set to -1 for no looping.
     */
    var loopCount: Int

    /**
     * The maximum number of colors in each frame, capped to [GIF_MAX_COLORS].
     */
    var maxColors: Int

    /**
     * The [ColorQuantizer] to use for reducing the number of colors in each frame to [maxColors].
     */
    var colorQuantizer: ColorQuantizer

    /**
     * The [ColorSimilarityChecker] to use for determining if two frames are similar
     * enough to merge.
     */
    var colorSimilarityChecker: ColorSimilarityChecker

    /**
     * An optional comment to include in the GIF comment block metadata.
     */
    var comment: String

    /**
     * The solid RGB color to use for filling in pixels with partial alpha transparency,
     * as GIFs do not support partial transparency.
     *
     * Set to -1 to disable filling.
     */
    var alphaFill: Int

    /**
     * Whether to crop the transparent pixels from the edges of each frame.
     * This can reduce the size of the GIF by a small amount.
     */
    var cropTransparent: Boolean

    /**
     * The minimum duration for each frame in centiseconds.
     * Setting this to a value less than [GIF_MINIMUM_FRAME_DURATION_CENTISECONDS] can result in the GIF being played
     * slower than expected on some GIF viewers.
     */
    var minimumFrameDurationCentiseconds: Int

    /**
     * The maximum number of frames that can be processed concurrently at the same time.
     * Used when creating a `ParallelGifEncoder`.
     */
    var maxConcurrency: Int

    /**
     * The [CoroutineScope] in which the concurrent encoding operations will run.
     * Used when creating a `ParallelGifEncoder`.
     */
    var coroutineScope: CoroutineScope

    /**
     * The [CoroutineContext] to use for writing to the [sink].
     * Used when creating a `ParallelGifEncoder`.
     */
    var ioContext: CoroutineContext

    /**
     * Builds a [GifEncoder] with the specified parameters.
     *
     * @param onFrameWritten A callback that is invoked after each frame is written,
     * providing the number of frames written and the total duration written so far.
     * This can be used to track progress or update a UI.
     *
     * @return The constructed [GifEncoder].
     */
    fun build(
        onFrameWritten: (
            framesWritten: Int,
            writtenDuration: Duration,
        ) -> Unit = { _, _ -> },
    ): GifEncoder
}
