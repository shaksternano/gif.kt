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

abstract class AsyncGifEncoder(
    private val sink: Sink,
    transparencyColorTolerance: Double,
    quantizedTransparencyColorTolerance: Double,
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
        transparencyColorTolerance,
        quantizedTransparencyColorTolerance,
        loopCount,
        maxColors,
        colorQuantizer,
        colorSimilarityChecker,
        comment,
        alphaFill,
        cropTransparent,
        minimumFrameDurationCentiseconds,
    )

    protected val optimizeQuantizedTransparency: Boolean = baseEncoder.optimizeQuantizedTransparency
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

    suspend fun writeFrame(
        image: IntArray,
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
            image,
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
        baseEncoder.writeOrOptimizeGifImage(
            imageData,
            originalImage,
            durationCentiseconds,
            disposalMethod,
            optimizedPreviousFrame,
            encodeAndWriteImage = { imageData1, durationCentiseconds1, disposalMethod1 ->
                encodeAndWriteImage(imageData1, durationCentiseconds1, disposalMethod1)
            },
        )
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

    protected open suspend fun wrapIo(block: () -> Unit) = block()

    protected open suspend fun onFrameWritten(framesWritten: Int, writtenDuration: Duration) = Unit

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

    protected data class QuantizeInput(
        val optimizedImage: Image,
        val originalImage: Image,
        val durationCentiseconds: Int,
        val disposalMethod: DisposalMethod,
        val optimizedPreviousFrame: Boolean,
    )

    protected data class QuantizeOutput(
        val quantizedImageData: QuantizedImageData,
        val originalImage: Image,
        val durationCentiseconds: Int,
        val disposalMethod: DisposalMethod,
        val optimizedPreviousFrame: Boolean,
    )

    protected data class EncodeInput(
        val imageData: QuantizedImageData,
        val durationCentiseconds: Int,
        val disposalMethod: DisposalMethod,
    )

    protected data class EncodeOutput(
        val source: Source,
        val duration: Duration,
    )
}

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
