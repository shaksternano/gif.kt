package com.shakster.gifkt

import com.shakster.gifkt.internal.AsyncExecutor
import com.shakster.gifkt.internal.BaseGifEncoder
import com.shakster.gifkt.internal.quantizeImage
import com.shakster.gifkt.internal.writeQuantizedGifImage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.io.IOException
import kotlinx.io.Sink
import kotlinx.io.Source
import kotlin.concurrent.atomics.AtomicReference
import kotlin.concurrent.atomics.ExperimentalAtomicApi
import kotlin.time.Duration

/**
 * An abstract class for implementing asynchronous GIF encoders.
 *
 * Implementors of this class should provide implementations for [quantizeImage] and [encodeGifImage],
 * and optionally override [wrapIo] and [onFrameWritten].
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
 */
abstract class AsyncGifEncoder(
    private val sink: Sink,
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
    coroutineScope: CoroutineScope,
) : SuspendClosable {

    init {
        require(maxConcurrency > 0) {
            "maxConcurrency must be positive: $maxConcurrency"
        }
    }

    private val baseEncoder: BaseGifEncoder = BaseGifEncoder(
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
    )

    /**
     * Whether to perform transparency optimization after quantization.
     */
    protected val optimizeQuantizedTransparency: Boolean = baseEncoder.optimizeQuantizedTransparency

    /**
     * The maximum number of colors in each frame.
     */
    protected val maxColors: Int = baseEncoder.maxColors

    @OptIn(ExperimentalAtomicApi::class)
    private val throwableReference: AtomicReference<Throwable?> = AtomicReference(null)

    private val quantizeExecutor: AsyncExecutor<QuantizeInput, QuantizeOutput> =
        AsyncExecutor(
            maxConcurrency = maxConcurrency,
            scope = coroutineScope,
            task = ::quantizeImage,
            onOutput = ::writeOrOptimizeGifImage,
        )

    private val encodeExecutor: AsyncExecutor<EncodeInput, EncodeOutput> =
        AsyncExecutor(
            maxConcurrency = maxConcurrency,
            scope = coroutineScope,
            task = ::encodeGifImage,
            onOutput = ::transferToSink,
        )

    private val writtenFrameNotifications: Channel<Duration> = Channel(capacity = Channel.UNLIMITED)

    private val writtenFrameListener: Job = coroutineScope.launch {
        var framesWritten = 0
        var writtenDuration = Duration.ZERO
        for (duration in writtenFrameNotifications) {
            framesWritten++
            writtenDuration += duration
            try {
                onFrameWritten(framesWritten, writtenDuration)
            } catch (t: Throwable) {
                val exception = Exception("Error running onFrameWritten callback", t)
                @OptIn(ExperimentalAtomicApi::class)
                throwableReference.compareAndSet(null, exception)
                break
            }
        }
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
    suspend fun writeFrame(
        argb: IntArray,
        width: Int,
        height: Int,
        duration: Duration,
    ) {
        @OptIn(ExperimentalAtomicApi::class)
        val throwable = throwableReference.load()
        if (throwable != null) {
            throw createException(throwable)
        }

        val written = baseEncoder.writeFrame(
            argb,
            width,
            height,
            duration,
            quantizeAndWriteFrame = { optimizedImage, originalImage, durationCentiseconds, disposalMethod, optimizedPreviousFrame ->
                quantizeAndWriteFrame(
                    optimizedImage,
                    originalImage,
                    durationCentiseconds,
                    disposalMethod,
                    optimizedPreviousFrame,
                )
            },
            wrapIo = {
                wrapIo(it)
            },
        )

        // Account for frames that have been merged due to similarity.
        if (!written) {
            writtenFrameNotifications.send(Duration.ZERO)
        }
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
    suspend fun writeFrame(frame: ImageFrame) {
        writeFrame(
            frame.argb,
            frame.width,
            frame.height,
            frame.duration,
        )
    }

    private suspend fun quantizeAndWriteFrame(
        optimizedImage: Image,
        originalImage: Image,
        durationCentiseconds: Int,
        disposalMethod: DisposalMethod,
        optimizedPreviousFrame: Boolean,
    ) {
        quantizeExecutor.submit(
            QuantizeInput(
                optimizedImage,
                originalImage,
                durationCentiseconds,
                disposalMethod,
                optimizedPreviousFrame,
            )
        )
    }

    /**
     * Reduces the number of colors in the given image to a maximum of [maxColors] colors.
     *
     * @param input The [QuantizeInput] containing the image to quantize and other parameters.
     *
     * @return The [QuantizeOutput] containing the quantized image data and other parameters.
     */
    protected abstract suspend fun quantizeImage(input: QuantizeInput): QuantizeOutput

    private suspend fun writeOrOptimizeGifImage(result: Result<QuantizeOutput>) {
        val error = result.exceptionOrNull()
        if (error != null) {
            @OptIn(ExperimentalAtomicApi::class)
            throwableReference.compareAndSet(null, error)
            return
        }
        val output = result.getOrThrow()
        writeOrOptimizeGifImage(
            output.quantizedImageData,
            output.originalImage,
            output.durationCentiseconds,
            output.disposalMethod,
            output.optimizedPreviousFrame,
        )
    }

    private suspend fun writeOrOptimizeGifImage(
        imageData: QuantizedImageData,
        originalImage: Image,
        durationCentiseconds: Int,
        disposalMethod: DisposalMethod,
        optimizedPreviousFrame: Boolean,
    ) {
        val written = baseEncoder.writeOrOptimizeGifImage(
            imageData,
            originalImage,
            durationCentiseconds,
            disposalMethod,
            optimizedPreviousFrame,
            encodeAndWriteImage = { imageData1, durationCentiseconds1, disposalMethod1 ->
                encodeAndWriteImage(imageData1, durationCentiseconds1, disposalMethod1)
            },
        )

        // Account for frames that have been merged due to similarity.
        if (!written) {
            writtenFrameNotifications.send(Duration.ZERO)
        }
    }

    private suspend fun encodeAndWriteImage(
        imageData: QuantizedImageData,
        durationCentiseconds: Int,
        disposalMethod: DisposalMethod,
    ) {
        encodeExecutor.submit(
            EncodeInput(
                imageData,
                durationCentiseconds,
                disposalMethod,
            )
        )
    }

    /**
     * Encodes the given image data to a GIF frame.
     *
     * @param input The [EncodeInput] containing the image data and other parameters.
     *
     * @return The [EncodeOutput] containing the encoded GIF frame and other parameters.
     */
    protected abstract suspend fun encodeGifImage(input: EncodeInput): EncodeOutput

    private suspend fun transferToSink(result: Result<EncodeOutput>) {
        val error = result.exceptionOrNull()
        if (error != null) {
            @OptIn(ExperimentalAtomicApi::class)
            throwableReference.compareAndSet(null, error)
            return
        }
        val output = result.getOrThrow()
        wrapIo {
            output.source.transferTo(sink)
        }
        writtenFrameNotifications.send(output.duration)
    }

    /**
     * Wraps [sink] operations in order to make them suspending.
     */
    protected open suspend fun wrapIo(block: () -> Unit) = block()

    /**
     * Called after each frame is written to the GIF.
     *
     * @param framesWritten The number of frames written so far.
     *
     * @param writtenDuration The total duration of all frames written so far.
     */
    protected open suspend fun onFrameWritten(framesWritten: Int, writtenDuration: Duration) = Unit

    /**
     * Closes the encoder, ensuring all data is written.
     * Closing the encoder also closes the underlying [sink].
     *
     * @throws IOException If an I/O error occurs.
     */
    override suspend fun close() {
        var closeThrowable: Throwable? = null
        try {
            quantizeExecutor.close()
            baseEncoder.close(
                quantizeAndWriteFrame = { optimizedImage, originalImage, durationCentiseconds, disposalMethod, optimizedPreviousFrame ->
                    writeOrOptimizeGifImage(
                        baseEncoder.quantizeImage(optimizedImage),
                        originalImage,
                        durationCentiseconds,
                        disposalMethod,
                        optimizedPreviousFrame,
                    )
                },
                encodeAndWriteImage = { imageData, durationCentiseconds, disposalMethod ->
                    encodeAndWriteImage(imageData, durationCentiseconds, disposalMethod)
                },
                afterFinalWrite = {
                    encodeExecutor.close()
                },
                wrapIo = {
                    wrapIo {
                        it()
                    }
                },
            )
            writtenFrameNotifications.close()
            writtenFrameListener.join()
        } catch (t: Throwable) {
            closeThrowable = t
            throw t
        } finally {
            @OptIn(ExperimentalAtomicApi::class)
            val throwable = throwableReference.load()
            if (throwable != null) {
                val exception = createException(throwable)
                if (closeThrowable == null) {
                    throw exception
                } else {
                    closeThrowable.addSuppressed(exception)
                }
            }
        }
    }

    private fun createException(cause: Throwable): IOException {
        return IOException("Error while writing GIF frame", cause)
    }

    /**
     * Input data for the quantization process.
     *
     * @param optimizedImage The optimized image to quantize.
     *
     * @param originalImage The original image to use for reference.
     *
     * @param durationCentiseconds The duration of the frame in centiseconds.
     *
     * @param disposalMethod The disposal method to use for the frame.
     *
     * @param optimizedPreviousFrame Whether the previous frame was optimized.
     */
    protected data class QuantizeInput(
        val optimizedImage: Image,
        val originalImage: Image,
        val durationCentiseconds: Int,
        val disposalMethod: DisposalMethod,
        val optimizedPreviousFrame: Boolean,
    )

    /**
     * Output data for the quantization process.
     *
     * @param quantizedImageData The quantized image data.
     *
     * @param originalImage The original image used for reference.
     *
     * @param durationCentiseconds The duration of the frame in centiseconds.
     *
     * @param disposalMethod The disposal method used for the frame.
     *
     * @param optimizedPreviousFrame Whether the previous frame was optimized.
     */
    protected data class QuantizeOutput(
        val quantizedImageData: QuantizedImageData,
        val originalImage: Image,
        val durationCentiseconds: Int,
        val disposalMethod: DisposalMethod,
        val optimizedPreviousFrame: Boolean,
    )

    /**
     * Input data for the GIF encoding process.
     *
     * @param imageData The image data to encode.
     *
     * @param durationCentiseconds The duration of the frame in centiseconds.
     *
     * @param disposalMethod The disposal method to use for the frame.
     */
    protected data class EncodeInput(
        val imageData: QuantizedImageData,
        val durationCentiseconds: Int,
        val disposalMethod: DisposalMethod,
    )

    /**
     * Output data for the GIF encoding process.
     *
     * @param source The [Source] containing the encoded GIF frame data.
     *
     * @param duration The duration of the frame.
     */
    protected data class EncodeOutput(
        val source: Source,
        val duration: Duration,
    )
}

/**
 * Quantizes the given image to a maximum number of colors.
 *
 * @param image The image to quantize.
 *
 * @param maxColors The maximum number of colors in the quantized image.
 *
 * @param quantizer The [ColorQuantizer] to use for quantization.
 *
 * @param forceTransparency Whether to reserve a color for transparency.
 *
 * @return The quantized image data.
 */
fun quantizeGifImage(
    image: Image,
    maxColors: Int,
    quantizer: ColorQuantizer,
    forceTransparency: Boolean,
): QuantizedImageData {
    return quantizeImage(
        image,
        maxColors,
        quantizer,
        forceTransparency,
    )
}

/**
 * Encodes the given quantized image data to a GIF frame and writes it to the sink.
 *
 * @receiver The [Sink] to write the GIF data to.
 *
 * @param imageData The image data to write.
 *
 * @param durationCentiseconds The duration of the frame in centiseconds.
 *
 * @param disposalMethod The disposal method to use for the frame.
 */
fun Sink.writeGifImage(
    imageData: QuantizedImageData,
    durationCentiseconds: Int,
    disposalMethod: DisposalMethod,
) {
    writeQuantizedGifImage(
        imageData,
        durationCentiseconds,
        disposalMethod,
    )
}
