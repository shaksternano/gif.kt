package com.shakster.gifkt

import com.shakster.gifkt.internal.BaseParallelGifEncoder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.future.future
import kotlinx.io.IOException
import kotlinx.io.Sink
import java.awt.image.BufferedImage
import java.util.concurrent.CompletableFuture
import kotlin.coroutines.CoroutineContext
import kotlin.time.Duration
import kotlin.time.toKotlinDuration
import java.time.Duration as JavaDuration

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
 * encoder.writeFrame(argb1, width1, height1, duration1)
 * encoder.writeFrame(argb2, width2, height2, duration2)
 *
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
 * @param maxColors The maximum number of colors in each frame, capped to [GIF_MAX_COLORS].
 *
 * @param colorQuantizer The [ColorQuantizer] to use for reducing the number of colors in each frame to [maxColors].
 *
 * @param colorSimilarityChecker The [ColorSimilarityChecker] to use for determining if two frames are similar
 * enough to merge.
 *
 * @param comment An optional comment to include in the GIF comment block metadata.
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
 * Setting this to a value less than [GIF_MINIMUM_FRAME_DURATION_CENTISECONDS] can result in the GIF being played
 * slower than expected on some GIF viewers.
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
 */
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
     * @throws IllegalArgumentException If [width] x [height] is not equal to [argb].[size][IntArray.size].
     *
     * @throws IOException If an I/O error occurs.
     */
    @Throws(IOException::class)
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
     * @throws IllegalArgumentException If [width] x [height] is not equal to [argb].[size][IntArray.size].
     *
     * @throws IOException If an I/O error occurs.
     */
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

    /**
     * Writes a single frame to the GIF.
     * The frame may be skipped if the [duration] is below [minimumFrameDurationCentiseconds],
     * or if the frame is the same as or similar enough to the previous frame,
     * determined by [colorDifferenceTolerance], [quantizedColorDifferenceTolerance],
     * and [colorSimilarityChecker].
     *
     * @param image The [BufferedImage] containing the pixel data of the frame.
     *
     * @param duration The duration of the frame.
     *
     * @throws IOException If an I/O error occurs.
     */
    @Throws(IOException::class)
    suspend fun writeFrame(image: BufferedImage, duration: Duration) {
        baseEncoder.writeFrame(
            image.rgb,
            image.width,
            image.height,
            duration,
        )
    }

    /**
     * Writes a single frame to the GIF.
     * The frame may be skipped if the [duration] is below [minimumFrameDurationCentiseconds],
     * or if the frame is the same as or similar enough to the previous frame,
     * determined by [colorDifferenceTolerance], [quantizedColorDifferenceTolerance],
     * and [colorSimilarityChecker].
     *
     * @param image The [BufferedImage] containing the pixel data of the frame.
     *
     * @param duration The duration of the frame.
     *
     * @throws IOException If an I/O error occurs.
     */
    @Throws(IOException::class)
    suspend fun writeFrame(image: BufferedImage, duration: JavaDuration) {
        baseEncoder.writeFrame(
            image.rgb,
            image.width,
            image.height,
            duration.toKotlinDuration(),
        )
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
    @Throws(IOException::class)
    actual suspend fun writeFrame(frame: ImageFrame) {
        baseEncoder.writeFrame(frame)
    }

    /**
     * Writes a single frame to the GIF asynchronously.
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
     * @return A [CompletableFuture] that completes when the frame has been submitted for encoding.
     * The future will complete exceptionally with an [Exception] if an error occurs.
     */
    fun writeFrameFuture(
        argb: IntArray,
        width: Int,
        height: Int,
        duration: Duration,
    ): CompletableFuture<Void> {
        return coroutineScope.future {
            baseEncoder.writeFrame(argb, width, height, duration)
        }.thenAccept { }
    }

    /**
     * Writes a single frame to the GIF asynchronously.
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
     * @return A [CompletableFuture] that completes when the frame has been submitted for encoding.
     * The future will complete exceptionally with an [Exception] if an error occurs.
     */
    fun writeFrameFuture(
        argb: IntArray,
        width: Int,
        height: Int,
        duration: JavaDuration,
    ): CompletableFuture<Void> {
        return coroutineScope.future {
            baseEncoder.writeFrame(
                argb,
                width,
                height,
                duration.toKotlinDuration(),
            )
        }.thenAccept { }
    }

    /**
     * Writes a single frame to the GIF asynchronously.
     * The frame may be skipped if the [duration] is below [minimumFrameDurationCentiseconds],
     * or if the frame is the same as or similar enough to the previous frame,
     * determined by [colorDifferenceTolerance], [quantizedColorDifferenceTolerance],
     * and [colorSimilarityChecker].
     *
     * @param image The [BufferedImage] containing the pixel data of the frame.
     *
     * @param duration The duration of the frame.
     *
     * @return A [CompletableFuture] that completes when the frame has been submitted for encoding.
     * The future will complete exceptionally with an [Exception] if an error occurs.
     */
    fun writeFrameFuture(image: BufferedImage, duration: Duration): CompletableFuture<Void> {
        return coroutineScope.future {
            baseEncoder.writeFrame(
                image.rgb,
                image.width,
                image.height,
                duration,
            )
        }.thenAccept { }
    }

    /**
     * Writes a single frame to the GIF asynchronously.
     * The frame may be skipped if the [duration] is below [minimumFrameDurationCentiseconds],
     * or if the frame is the same as or similar enough to the previous frame,
     * determined by [colorDifferenceTolerance], [quantizedColorDifferenceTolerance],
     * and [colorSimilarityChecker].
     *
     * @param image The [BufferedImage] containing the pixel data of the frame.
     *
     * @param duration The duration of the frame.
     *
     * @return A [CompletableFuture] that completes when the frame has been submitted for encoding.
     * The future will complete exceptionally with an [Exception] if an error occurs.
     */
    fun writeFrameFuture(image: BufferedImage, duration: JavaDuration): CompletableFuture<Void> {
        return coroutineScope.future {
            baseEncoder.writeFrame(
                image.rgb,
                image.width,
                image.height,
                duration.toKotlinDuration(),
            )
        }.thenAccept { }
    }

    /**
     * Writes a single frame to the GIF asynchronously.
     * The frame may be skipped if the duration is below [minimumFrameDurationCentiseconds],
     * or if the frame is the same as or similar enough to the previous frame,
     * determined by [colorDifferenceTolerance], [quantizedColorDifferenceTolerance],
     * and [colorSimilarityChecker].
     *
     * @param frame The [ImageFrame] containing the argb data, dimensions, and duration of the frame.
     *
     * @return A [CompletableFuture] that completes when the frame has been submitted for encoding.
     * The future will complete exceptionally with an [Exception] if an error occurs.
     */
    fun writeFrameFuture(frame: ImageFrame): CompletableFuture<Void> {
        return coroutineScope.future {
            baseEncoder.writeFrame(frame)
        }.thenAccept { }
    }

    /**
     * Closes the encoder, ensuring all data is written.
     * Closing the encoder also closes the underlying [sink].
     *
     * @throws IOException If an I/O error occurs.
     */
    @Throws(IOException::class)
    actual override suspend fun close() {
        baseEncoder.close()
    }

    /**
     * Closes the encoder asynchronously, ensuring all data is written.
     * Closing the encoder also closes the underlying [sink].
     *
     * @return A [CompletableFuture] that completes when the encoder has been closed.
     * The future will complete exceptionally with an [Exception] if an error occurs.
     */
    fun closeFuture(): CompletableFuture<Void> {
        return coroutineScope.future {
            baseEncoder.close()
        }.thenAccept { }
    }
}
