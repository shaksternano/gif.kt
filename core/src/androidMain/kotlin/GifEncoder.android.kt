package com.shakster.gifkt

import com.shakster.gifkt.internal.BaseSyncGifEncoder
import kotlinx.io.IOException
import kotlinx.io.Sink
import kotlinx.io.asSink
import kotlinx.io.buffered
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import java.io.File
import java.io.OutputStream
import kotlin.io.path.outputStream
import kotlin.time.Duration
import kotlin.time.toKotlinDuration
import java.nio.file.Path as JavaPath
import java.time.Duration as JavaDuration

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
actual class GifEncoder
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
     * @throws IOException If an I/O error occurs.
     */
    @Throws(IOException::class)
    fun writeFrame(
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
    actual fun writeFrame(frame: ImageFrame) {
        baseEncoder.writeFrame(frame)
    }

    /**
     * Closes the encoder, ensuring all data is written.
     * Closing the encoder also closes the underlying [sink].
     *
     * @throws IOException If an I/O error occurs.
     */
    @Throws(IOException::class)
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
        @JvmStatic
        actual fun builder(sink: Sink): GifEncoderBuilder {
            return GifEncoderBuilder(sink)
        }

        /**
         * Creates a new [GifEncoderBuilder] for configuring and building a [GifEncoder].
         *
         * @param outputStream The [OutputStream] to write the GIF data to.
         *
         * @return A [GifEncoderBuilder] instance for configuring the encoder.
         */
        @JvmStatic
        fun builder(outputStream: OutputStream): GifEncoderBuilder {
            return GifEncoderBuilder(outputStream.asSink().buffered())
        }

        /**
         * Creates a new [GifEncoderBuilder] for configuring and building a [GifEncoder].
         *
         * @param path The path of the file to write the GIF data to.
         *
         * @return A [GifEncoderBuilder] instance for configuring the encoder.
         *
         * @throws IOException If an I/O error occurs.
         */
        @JvmStatic
        @Throws(IOException::class)
        fun builder(path: Path): GifEncoderBuilder {
            return GifEncoderBuilder(SystemFileSystem.sink(path).buffered())
        }

        /**
         * Creates a new [GifEncoderBuilder] for configuring and building a [GifEncoder].
         *
         * @param path The path of the file to write the GIF data to.
         *
         * @return A [GifEncoderBuilder] instance for configuring the encoder.
         *
         * @throws IOException If an I/O error occurs.
         */
        @JvmStatic
        @Throws(IOException::class)
        fun builder(path: JavaPath): GifEncoderBuilder {
            return GifEncoderBuilder(path.outputStream().asSink().buffered())
        }

        /**
         * Creates a new [GifEncoderBuilder] for configuring and building a [GifEncoder].
         *
         * @param file The file to write the GIF data to.
         *
         * @return A [GifEncoderBuilder] instance for configuring the encoder.
         *
         * @throws IOException If an I/O error occurs.
         */
        @JvmStatic
        @Throws(IOException::class)
        fun builder(file: File): GifEncoderBuilder {
            return GifEncoderBuilder(file.outputStream().asSink().buffered())
        }
    }
}
