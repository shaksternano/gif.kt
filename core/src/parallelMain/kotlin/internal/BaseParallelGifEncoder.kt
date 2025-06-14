package com.shakster.gifkt.internal

import com.shakster.gifkt.ColorDistanceCalculator
import com.shakster.gifkt.ColorQuantizer
import com.shakster.gifkt.ImageFrame
import com.shakster.gifkt.SuspendClosable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.io.Buffer
import kotlinx.io.IOException
import kotlinx.io.Sink
import kotlin.concurrent.atomics.AtomicReference
import kotlin.concurrent.atomics.ExperimentalAtomicApi
import kotlin.coroutines.CoroutineContext
import kotlin.time.Duration

internal class BaseParallelGifEncoder(
    private val sink: Sink,
    transparencyColorTolerance: Double,
    quantizedTransparencyColorTolerance: Double,
    loopCount: Int,
    maxColors: Int,
    colorQuantizer: ColorQuantizer,
    colorDistanceCalculator: ColorDistanceCalculator,
    comment: String,
    alphaFill: Int,
    cropTransparent: Boolean,
    minimumFrameDurationCentiseconds: Int,
    maxConcurrency: Int,
    coroutineScope: CoroutineScope,
    private val ioContext: CoroutineContext,
    private val onFrameWritten: suspend (
        framesWritten: Int,
        writtenDuration: Duration,
    ) -> Unit,
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
        colorDistanceCalculator,
        comment,
        alphaFill,
        cropTransparent,
        minimumFrameDurationCentiseconds,
    )

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
                withContext(ioContext) {
                    it()
                }
            },
        )

        // Account for frames that have been merged due to similarity.
        if (!written) {
            writtenFrameNotifications.send(Duration.ZERO)
        }
    }

    suspend fun writeFrame(frame: ImageFrame) =
        writeFrame(
            frame.argb,
            frame.width,
            frame.height,
            frame.duration,
        )

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

    private fun quantizeImage(input: QuantizeInput): QuantizeOutput {
        val (
            image,
            originalImage,
            durationCentiseconds,
            disposalMethod,
            optimizedPreviousFrame,
        ) = input
        return QuantizeOutput(
            baseEncoder.getImageData(image),
            originalImage,
            durationCentiseconds,
            disposalMethod,
            optimizedPreviousFrame,
        )
    }

    private suspend fun writeOrOptimizeGifImage(output: Result<QuantizeOutput>) {
        val error = output.exceptionOrNull()
        if (error != null) {
            @OptIn(ExperimentalAtomicApi::class)
            throwableReference.compareAndSet(null, error)
            return
        }
        val (
            imageData,
            originalImage,
            durationCentiseconds,
            disposalMethod,
            optimizedPreviousFrame,
        ) = output.getOrThrow()
        writeOrOptimizeGifImage(
            imageData,
            originalImage,
            durationCentiseconds,
            disposalMethod,
            optimizedPreviousFrame,
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

    private fun encodeGifImage(input: EncodeInput): EncodeOutput {
        val (
            imageData,
            durationCentiseconds,
            disposalMethod,
        ) = input
        val buffer = Buffer()
        buffer.writeGifImage(
            imageData,
            durationCentiseconds,
            disposalMethod,
        )
        return EncodeOutput(buffer, input.durationCentiseconds.centiseconds)
    }

    private suspend fun transferToSink(result: Result<EncodeOutput>) {
        val error = result.exceptionOrNull()
        if (error != null) {
            @OptIn(ExperimentalAtomicApi::class)
            throwableReference.compareAndSet(null, error)
            return
        }
        val output = result.getOrThrow()
        withContext(ioContext) {
            output.buffer.transferTo(sink)
        }
        writtenFrameNotifications.send(output.duration)
    }

    override suspend fun close() {
        var closeThrowable: Throwable? = null
        try {
            quantizeExecutor.close()
            baseEncoder.close(
                quantizeAndWriteFrame = { optimizedImage, originalImage, durationCentiseconds, disposalMethod, optimizedPreviousFrame ->
                    writeOrOptimizeGifImage(
                        baseEncoder.getImageData(optimizedImage),
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
                    withContext(ioContext) {
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

    private data class QuantizeInput(
        val optimizedImage: Image,
        val originalImage: Image,
        val durationCentiseconds: Int,
        val disposalMethod: DisposalMethod,
        val optimizedPreviousFrame: Boolean,
    )

    private data class QuantizeOutput(
        val data: QuantizedImageData,
        val originalImage: Image,
        val durationCentiseconds: Int,
        val disposalMethod: DisposalMethod,
        val optimizedPreviousFrame: Boolean,
    )

    private data class EncodeInput(
        val imageData: QuantizedImageData,
        val durationCentiseconds: Int,
        val disposalMethod: DisposalMethod,
    )

    private data class EncodeOutput(
        val buffer: Buffer,
        val duration: Duration,
    )
}
