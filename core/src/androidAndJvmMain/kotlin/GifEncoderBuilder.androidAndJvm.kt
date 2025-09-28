@file:JvmName("GifEncoderBuilderJvm")

package com.shakster.gifkt

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.future.await
import kotlinx.io.Sink
import java.util.concurrent.CompletionStage
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.time.Duration
import kotlin.time.toJavaDuration
import java.time.Duration as JavaDuration

/**
 * A builder for creating [GifEncoder] and [ParallelGifEncoder]s.
 *
 * @param sink The [Sink] to write the GIF data to.
 */
actual class GifEncoderBuilder actual constructor(
    private val sink: Sink,
) {

    /**
     * The tolerance for color difference used by [colorSimilarityChecker] when performing transparency
     * optimization. This optimization works by making pixels that are similar to the pixel in the
     * previous frame transparent, resulting in only pixels that are different being saved. Higher values
     * will result in a smaller file size at the cost of visual artifacts. This optimization is most
     * effective on animations with large static areas.
     *
     * The default value of 0 slightly reduces file size with no visual quality loss.
     *
     * A value of around 0.01 provides a good tradeoff between file size and quality.
     *
     * Set to -1 to disable transparency optimization.
     */
    actual var colorDifferenceTolerance: Double = 0.0

    /**
     * The tolerance for color difference used by [colorSimilarityChecker] when performing transparency
     * optimization after quantization. This optimization works by making pixels that are similar to the
     * pixel in the previous frame transparent, resulting in only pixels that are different being saved.
     * Higher values will result in a smaller file size at the cost of visual artifacts. This optimization
     * is most effective on animations with large static areas.
     *
     * A value of around 0.02 provides a good tradeoff between file size and quality.
     *
     * Set to -1 to disable transparency optimization.
     */
    actual var quantizedColorDifferenceTolerance: Double = -1.0

    /**
     * The number of times the GIF should loop.
     *
     * Set to 0 for infinite looping.
     *
     * Set to -1 for no looping.
     */
    actual var loopCount: Int = 0

    /**
     * The maximum number of colors in each frame.
     *
     * Must be between 1 and 256 inclusive.
     */
    actual var maxColors: Int = GIF_MAX_COLORS

    /**
     * The [ColorQuantizer] to use for reducing the number of colors in each frame to [maxColors].
     */
    actual var colorQuantizer: ColorQuantizer = ColorQuantizer.NEU_QUANT

    /**
     * The [ColorSimilarityChecker] to use for determining if two frames are similar
     * enough to merge.
     */
    actual var colorSimilarityChecker: ColorSimilarityChecker = ColorSimilarityChecker.EUCLIDEAN_LUMINANCE_WEIGHTING

    /**
     * An optional comment to include in the GIF comment block metadata.
     */
    actual var comment: String = ""

    /**
     * The alpha threshold for a pixel to be considered transparent.
     * Pixels with an alpha value equal to or less than this value will be treated as fully transparent.
     *
     * Must be between 0 and 255 inclusive.
     */
    actual var transparentAlphaThreshold: Int = 20

    /**
     * The solid RGB color to use for filling in pixels with partial alpha transparency,
     * as GIFs do not support partial transparency.
     *
     * Set to -1 to disable filling.
     */
    actual var alphaFill: Int = -1

    /**
     * Whether to crop the transparent pixels from the edges of each frame.
     * This can reduce the size of the GIF by a small amount.
     */
    actual var cropTransparent: Boolean = true

    /**
     * The minimum duration for each frame in centiseconds.
     * Setting this to a value less than 2 can result in the GIF being played
     * slower than expected on some GIF viewers.
     *
     * Must be positive.
     */
    actual var minimumFrameDurationCentiseconds: Int = GIF_MINIMUM_FRAME_DURATION_CENTISECONDS

    /**
     * The maximum number of frames that can be processed concurrently at the same time.
     * Used when creating a [ParallelGifEncoder].
     */
    var maxConcurrency: Int = 2

    /**
     * The [CoroutineScope] in which the concurrent encoding operations will run.
     * Used when creating a [ParallelGifEncoder].
     */
    var coroutineScope: CoroutineScope = CoroutineScope(EmptyCoroutineContext)

    /**
     * The [CoroutineContext] to use for writing to the [sink].
     * Used when creating a [ParallelGifEncoder].
     */
    var ioContext: CoroutineContext = EmptyCoroutineContext

    /**
     * Builds a [GifEncoder] with the specified parameters.
     *
     * @param onFrameWritten A callback that is invoked after each frame is written,
     * providing the number of frames written and the total duration of all the frames written so far.
     * This can be used to track progress or update a UI.
     *
     * @return The constructed [GifEncoder].
     *
     * @throws IllegalArgumentException If any of the builder parameters are invalid.
     */
    @JvmOverloads
    actual fun build(
        onFrameWritten: (
            framesWritten: Int,
            writtenDuration: Duration,
        ) -> Unit,
    ): GifEncoder {
        return GifEncoder(
            sink,
            colorDifferenceTolerance,
            quantizedColorDifferenceTolerance,
            loopCount,
            maxColors,
            colorQuantizer,
            colorSimilarityChecker,
            comment,
            transparentAlphaThreshold,
            alphaFill,
            cropTransparent,
            minimumFrameDurationCentiseconds,
            onFrameWritten,
        )
    }

    /**
     * Builds a [GifEncoder] with the specified parameters.
     *
     * @param onFrameWritten A callback that is invoked after each frame is written,
     * providing the number of frames written and the total duration of all the frames written so far.
     * This can be used to track progress or update a UI.
     *
     * @return The constructed [GifEncoder].
     *
     * @throws IllegalArgumentException If any of the builder parameters are invalid.
     */
    fun buildJavaCallback(
        onFrameWritten: OnFrameWrittenCallback,
    ): GifEncoder {
        return build { framesWritten, writtenDuration ->
            onFrameWritten.onFrameWritten(
                framesWritten,
                writtenDuration.toJavaDuration(),
            )
        }
    }

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
    @JvmOverloads
    fun buildParallel(
        onFrameWritten: suspend (
            framesWritten: Int,
            writtenDuration: Duration,
        ) -> Unit = { _, _ -> },
    ): ParallelGifEncoder {
        return ParallelGifEncoder(
            sink,
            colorDifferenceTolerance,
            quantizedColorDifferenceTolerance,
            loopCount,
            maxColors,
            colorQuantizer,
            colorSimilarityChecker,
            comment,
            transparentAlphaThreshold,
            alphaFill,
            cropTransparent,
            minimumFrameDurationCentiseconds,
            maxConcurrency,
            coroutineScope,
            ioContext,
            onFrameWritten,
        )
    }

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
    fun buildParallelJavaCallback(
        onFrameWritten: OnFrameWrittenCallback,
    ): ParallelGifEncoder {
        return buildParallel { framesWritten, writtenDuration ->
            onFrameWritten.onFrameWritten(
                framesWritten,
                writtenDuration.toJavaDuration(),
            )
        }
    }

    /**
     * Builds a [ParallelGifEncoder] with the specified parameters.
     *
     * @param onFrameWritten An asynchronous callback that is invoked after each frame is written,
     * providing the number of frames written and the total duration of all the frames written so far,
     * returning a [CompletionStage] that completes when the callback is done.
     * This can be used to track progress or update a UI.
     *
     * @return The constructed [ParallelGifEncoder].
     *
     * @throws IllegalArgumentException If any of the builder parameters are invalid.
     */
    fun buildParallelFutureCallback(
        onFrameWritten: (
            framesWritten: Int,
            writtenDuration: JavaDuration,
        ) -> CompletionStage<*>,
    ): ParallelGifEncoder {
        return buildParallel { framesWritten, writtenDuration ->
            onFrameWritten(
                framesWritten,
                writtenDuration.toJavaDuration(),
            ).await()
        }
    }
}

/**
 * The maximum number of frames that can be processed concurrently at the same time.
 * Used when creating a [ParallelGifEncoder].
 */
actual var GifEncoderBuilder.maxConcurrency: Int
    get() = maxConcurrency
    set(maxConcurrency) {
        this.maxConcurrency = maxConcurrency
    }

/**
 * The [CoroutineScope] in which the concurrent encoding operations will run.
 * Used when creating a [ParallelGifEncoder].
 */
actual var GifEncoderBuilder.coroutineScope: CoroutineScope
    get() = coroutineScope
    set(coroutineScope) {
        this.coroutineScope = coroutineScope
    }

/**
 * The [CoroutineContext] to use for writing to the [sink][GifEncoderBuilder.sink].
 * Used when creating a [ParallelGifEncoder].
 */
actual var GifEncoderBuilder.ioContext: CoroutineContext
    get() = ioContext
    set(ioContext) {
        this.ioContext = ioContext
    }

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
actual fun GifEncoderBuilder.buildParallel(
    onFrameWritten: suspend (
        framesWritten: Int,
        writtenDuration: Duration,
    ) -> Unit,
): ParallelGifEncoder {
    return buildParallel(onFrameWritten)
}
