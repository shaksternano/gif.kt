package com.shakster.gifkt

import com.shakster.gifkt.internal.BaseSyncGifEncoder
import kotlinx.io.IOException
import kotlinx.io.Sink
import kotlin.time.Duration

/**
 * A class for encoding GIF files.
 * The encoder must be closed after use to ensure all data is written correctly.
 *
 * Basic usage:
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
 * encoder.close()
 * ```
 *
 * @param sink The [Sink] to write the GIF data to.
 *
 * @param colorDifferenceTolerance The tolerance for color difference when performing transparency optimization.
 * Set to -1 to disable transparency optimization.
 *
 * @param quantizedColorDifferenceTolerance The tolerance for color difference when performing transparency
 * optimization after quantization. Set to -1 to disable post-quantization transparency optimization.
 *
 * @param loopCount The number of times the GIF should loop. Set to 0 for infinite looping.
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
 * as GIFs do not support partial transparency. Set to -1 to disable filling.
 *
 * @param cropTransparent Whether to crop the transparent pixels from the edges of each frame.
 * This can reduce the size of the GIF by a small amount.
 *
 * @param minimumFrameDurationCentiseconds The minimum duration for each frame in centiseconds.
 * Setting this to a value less than [GIF_MINIMUM_FRAME_DURATION_CENTISECONDS] can result in the GIF being played
 * slower than expected on some GIF viewers.
 *
 * @param onFrameWritten A callback that is invoked after each frame is written,
 * providing the number of frames written and the total duration written so far.
 * This can be used to track progress or update a UI.
 */
actual class GifEncoder actual constructor(
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
    onFrameWritten: (
        framesWritten: Int,
        writtenDuration: Duration,
    ) -> Unit,
) : AutoCloseable {

    private val baseEncoder: BaseSyncGifEncoder = BaseSyncGifEncoder(
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
        onFrameWritten,
    )

    /**
     * Writes a single frame to the GIF.
     *
     * @param argb The ARGB pixel data for the frame,
     * going row by row from top to bottom.
     *
     * @param width The width of the frame in pixels.
     *
     * @param height The height of the frame in pixels.
     *
     * @param duration The duration of the frame.
     *
     * @throws IOException If an I/O error occurs.
     */
    @Throws(IOException::class)
    actual fun writeFrame(
        argb: IntArray,
        width: Int,
        height: Int,
        duration: Duration,
    ) {
        baseEncoder.writeFrame(
            argb,
            width,
            height,
            duration,
        )
    }

    /**
     * Writes a single frame to the GIF.
     *
     * @param frame The [ImageFrame] containing the argb data, dimensions, and duration of the frame.
     *
     * @throws IOException If an I/O error occurs.
     */
    @Throws(IOException::class)
    actual fun writeFrame(frame: ImageFrame) {
        baseEncoder.writeFrame(frame)
    }

    /**
     * Closes the encoder, ensuring all data is written.
     * Closing the encoder also closes the underlying sink.
     *
     * @throws IOException If an I/O error occurs.
     */
    actual override fun close() {
        baseEncoder.close()
    }

    actual companion object {
        /**
         * Creates a new [GifEncoderBuilder] for configuring and building a [GifEncoder].
         *
         * @param sink The [Sink] to write the GIF data to.
         *
         * @return A [GifEncoderBuilder] instance for configuring the encoder.
         */
        actual fun builder(sink: Sink): GifEncoderBuilder {
            return GifEncoderBuilder(sink)
        }
    }
}
