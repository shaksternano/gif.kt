package com.shakster.gifkt

import kotlinx.io.IOException
import kotlinx.io.Sink
import kotlin.time.Duration

/**
 * The maximum number of colors allowed in a GIF frame.
 */
const val GIF_MAX_COLORS: Int = 256

/**
 * The minimum duration of a GIF frame in centiseconds that is supported by most GIF viewers.
 */
const val GIF_MINIMUM_FRAME_DURATION_CENTISECONDS: Int = 2

/**
 * A class for encoding GIF files.
 * The dimensions of the created GIF are determined by the first frame written.
 * Subsequent frames will have the same dimensions as the first frame,
 * cropping or padding the frame to fit if necessary.
 * The encoder must be closed after use to ensure all data is written correctly.
 *
 * Usage:
 * ```kotlin
 * // Obtain a Sink to write the GIF data to
 * val sink: Sink = ...
 * val encoder = GifEncoder(sink)
 *
 * val argb: IntArray = ...
 * val width: Int = ...
 * val height: Int = ...
 * val duration: Duration = ...
 * encoder.writeFrame(argb, width, height, duration)
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
 * @param onFrameWritten A callback that is invoked after each frame is written,
 * providing the number of frames written and the total duration of all the frames written so far.
 * This can be used to track progress or update a UI.
 */
expect class GifEncoder(
    sink: Sink,
    colorDifferenceTolerance: Double = 0.0,
    quantizedColorDifferenceTolerance: Double = -1.0,
    loopCount: Int = 0,
    maxColors: Int = GIF_MAX_COLORS,
    colorQuantizer: ColorQuantizer = ColorQuantizer.NEU_QUANT,
    colorSimilarityChecker: ColorSimilarityChecker = ColorSimilarityChecker.EUCLIDEAN_LUMINANCE_WEIGHTING,
    comment: String = "",
    alphaFill: Int = -1,
    cropTransparent: Boolean = true,
    minimumFrameDurationCentiseconds: Int = GIF_MINIMUM_FRAME_DURATION_CENTISECONDS,
    onFrameWritten: (
        framesWritten: Int,
        writtenDuration: Duration,
    ) -> Unit = { _, _ -> },
) : AutoCloseable {

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
    fun writeFrame(
        argb: IntArray,
        width: Int,
        height: Int,
        duration: Duration,
    )

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
    fun writeFrame(frame: ImageFrame)

    /**
     * Closes the encoder, ensuring all data is written.
     * Closing the encoder also closes the underlying [sink].
     *
     * @throws IOException If an I/O error occurs.
     */
    override fun close()

    companion object {
        /**
         * Creates a new [GifEncoderBuilder] for configuring and building a [GifEncoder].
         *
         * @param sink The [Sink] to write the GIF data to.
         *
         * @return A [GifEncoderBuilder] instance for configuring the encoder.
         */
        fun builder(sink: Sink): GifEncoderBuilder
    }
}
