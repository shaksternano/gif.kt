package com.shakster.gifkt

import com.shakster.gifkt.internal.BaseParallelGifEncoder
import kotlinx.coroutines.CoroutineScope
import kotlinx.io.IOException
import kotlinx.io.Sink
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.Duration

/**
 * A class for encoding GIF files.
 * This encoder supports writing multiple frames in parallel.
 * The dimensions of the created GIF are determined by the first frame written.
 * Subsequent frames will have the same dimensions as the first frame,
 * cropping or padding the frame to fit if necessary.
 * The encoder must be closed after use to ensure all data is written correctly.
 *
 * Usage:
 * ```kotlin
 * // Obtain a Sink to write the GIF data to
 * val sink: Sink = ...
 * // Use all available CPU cores for maximum encoding speed
 * val cpuCount: Int = ...
 * val encoder = ParallelGifEncoder(
 *     sink,
 *     maxConcurrency = cpuCount,
 *     ioContext = Dispatchers.IO,
 * )
 *
 * val argb1: IntArray = ...
 * val width1: Int = ...
 * val height1: Int = ...
 * val duration1: Duration = ...
 *
 * val argb2: IntArray = ...
 * val width2: Int = ...
 * val height2: Int = ...
 * val duration2: Duration = ...
 *
 * // Frames are encoded in parallel
 * // Suspending
 * encoder.writeFrame(argb1, width1, height1, duration1)
 * encoder.writeFrame(argb2, width2, height2, duration2)
 *
 * // Suspending
 * encoder.close()
 * ```
 *
 * @param sink The [Sink] to write the GIF data to.
 *
 * @param colorDifferenceTolerance The tolerance for color difference used by [colorSimilarityChecker]
 * when performing transparency optimization. This optimization works by making pixels that are similar to
 * the pixel in the previous frame transparent, resulting in only pixels that are different being saved.
 * Higher values will result in a smaller file size at the cost of visual artifacts. This optimization
 * is most effective on animations with large static areas.
 *
 * The default value of 0 slightly reduces file size with no visual quality loss.
 *
 * A value of around 0.01 provides a good tradeoff between file size and quality.
 *
 * Set to -1 to disable transparency optimization.
 *
 * @param quantizedColorDifferenceTolerance The tolerance for color difference used by [colorSimilarityChecker]
 * when performing transparency optimization after quantization. This optimization works by making pixels
 * that are similar to the pixel in the previous frame transparent, resulting in only pixels that are different
 * being saved. Higher values will result in a smaller file size at the cost of visual artifacts. This
 * optimization is most effective on animations with large static areas.
 *
 * A value of around 0.02 provides a good tradeoff between file size and quality.
 *
 * Set to -1 to disable transparency optimization.
 *
 * @param loopCount The number of times the GIF should loop.
 *
 * Set to 0 for infinite looping.
 *
 * Set to -1 for no looping.
 *
 * @param maxColors The maximum number of colors in each frame.
 *
 * Must be between 1 and 256 inclusive.
 *
 * @param colorQuantizer The [ColorQuantizer] to use for reducing the number of colors in each frame to [maxColors].
 *
 * @param colorSimilarityChecker The [ColorSimilarityChecker] to use for determining if two frames are similar
 * enough to merge.
 *
 * @param comment An optional comment to include in the GIF comment block metadata.
 *
 * @param transparentAlphaThreshold The alpha threshold for a pixel to be considered transparent.
 * Pixels with an alpha value equal to or less than this value will be treated as fully transparent.
 *
 * Must be between 0 and 255 inclusive.
 *
 * @param alphaFill The solid RGB color to use for filling in pixels with partial alpha transparency,
 * as GIFs do not support partial transparency.
 *
 * Set to -1 to disable filling.
 *
 * @param cropTransparent Whether to crop the transparent pixels from the edges of each frame.
 * This can reduce the size of the GIF by a small amount.
 *
 * @param minimumFrameDurationCentiseconds The minimum duration for each frame in centiseconds.
 * Setting this to a value less than 2 can result in the GIF being played
 * slower than expected on some GIF viewers.
 *
 * Must be positive.
 *
 * @param maxConcurrency The maximum number of frames that can be processed concurrently at the same time.
 *
 * @param coroutineScope The [CoroutineScope] in which the concurrent encoding operations will run.
 *
 * @param ioContext The [CoroutineContext] to use for writing to the [sink].
 *
 * @param onFrameWritten A callback that is invoked after each frame is written,
 * providing the number of frames written and the total duration of all the frames written so far.
 * This can be used to track progress or update a UI.
 *
 * @throws IllegalArgumentException If any of the parameters are invalid.
 */
actual class ParallelGifEncoder actual constructor(
    sink: Sink,
    colorDifferenceTolerance: Double,
    quantizedColorDifferenceTolerance: Double,
    loopCount: Int,
    maxColors: Int,
    colorQuantizer: ColorQuantizer,
    colorSimilarityChecker: ColorSimilarityChecker,
    comment: String,
    transparentAlphaThreshold: Int,
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

    /**
     * Writes a single frame to the GIF.
     * The frame may be skipped if the [duration] is below [minimumFrameDurationCentiseconds],
     * or if the frame is the same as or similar enough to the previous frame,
     * determined by [colorDifferenceTolerance], [quantizedColorDifferenceTolerance],
     * and [colorSimilarityChecker].
     *
     * @param argb The ARGB pixel data for the frame.
     * Each element in the array represents a pixel in ARGB format,
     * going row by row from top to bottom.
     *
     * @param width The width of the frame in pixels.
     *
     * @param height The height of the frame in pixels.
     *
     * @param duration The duration of the frame.
     *
     * @throws IllegalArgumentException If [width] x [height] is not equal to [argb].[size][IntArray.size]
     * or [duration] is negative.
     *
     * @throws IOException If an I/O error occurs.
     */
    @Throws(CancellationException::class, IOException::class)
    actual suspend fun writeFrame(
        argb: IntArray,
        width: Int,
        height: Int,
        duration: Duration,
    ) {
        baseEncoder.writeFrame(argb, width, height, duration)
    }

    /**
     * Writes a single frame to the GIF.
     * The frame may be skipped if the duration is below [minimumFrameDurationCentiseconds],
     * or if the frame is the same as or similar enough to the previous frame,
     * determined by [colorDifferenceTolerance], [quantizedColorDifferenceTolerance],
     * and [colorSimilarityChecker].
     *
     * @param frame The [ImageFrame] containing the argb data, dimensions, and duration of the frame.
     *
     * @throws IOException If an I/O error occurs.
     */
    @Throws(CancellationException::class, IOException::class)
    actual suspend fun writeFrame(frame: ImageFrame) {
        baseEncoder.writeFrame(frame)
    }

    /**
     * Closes the encoder, ensuring all data is written.
     * Closing the encoder also closes the underlying [sink].
     *
     * @throws IOException If an I/O error occurs.
     */
    actual override suspend fun close() {
        baseEncoder.close()
    }
}
