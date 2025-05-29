package com.shakster.gifkt

import com.shakster.gifkt.internal.*
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
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.time.Duration

class ParallelGifEncoder(
    private val sink: Sink,
    loopCount: Int = 0,
    maxColors: Int = GIF_MAX_COLORS,
    transparencyColorTolerance: Double = 0.0,
    optimizeTransparency: Boolean = true,
    quantizedTransparencyColorTolerance: Double = 0.0,
    optimizeQuantizedTransparency: Boolean = quantizedTransparencyColorTolerance > 0.0,
    cropTransparent: Boolean = true,
    alphaFill: Int = -1,
    comment: String = "",
    minimumFrameDurationCentiseconds: Int = GIF_MINIMUM_FRAME_DURATION_CENTISECONDS,
    quantizer: ColorQuantizer = NeuQuantizer.DEFAULT,
    maxConcurrency: Int = 2,
    coroutineScope: CoroutineScope = CoroutineScope(EmptyCoroutineContext),
    private val ioContext: CoroutineContext = EmptyCoroutineContext,
    private val onFrameWritten: suspend (index: Int) -> Unit = {},
) : SuspendClosable {

    init {
        require(maxConcurrency > 0) {
            "maxConcurrency must be positive: $maxConcurrency"
        }
    }

    private val baseEncoder: BaseGifEncoder = BaseGifEncoder(
        sink,
        loopCount,
        maxColors,
        transparencyColorTolerance,
        optimizeTransparency,
        quantizedTransparencyColorTolerance,
        optimizeQuantizedTransparency,
        cropTransparent,
        alphaFill,
        comment,
        minimumFrameDurationCentiseconds,
        quantizer,
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

    private val encodeExecutor: AsyncExecutor<EncodeInput, Buffer> =
        AsyncExecutor(
            maxConcurrency = maxConcurrency,
            scope = coroutineScope,
            task = ::encodeGifImage,
            onOutput = ::transferToSink,
        )

    private val writtenFrameNotifications: Channel<Unit> = Channel(capacity = Channel.UNLIMITED)
    private val writtenFrameListener: Job = coroutineScope.launch {
        var writtenFrameIndex = 0
        @Suppress("unused")
        for (unused in writtenFrameNotifications) {
            try {
                onFrameWritten(writtenFrameIndex)
            } catch (t: Throwable) {
                val exception = Exception("Error running onFrameWritten callback", t)
                @OptIn(ExperimentalAtomicApi::class)
                throwableReference.compareAndSet(null, exception)
            }
            writtenFrameIndex++
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
            writtenFrameNotifications.send(Unit)
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

    private fun encodeGifImage(input: EncodeInput): Buffer {
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
        return buffer
    }

    private suspend fun transferToSink(output: Result<Buffer>) {
        val error = output.exceptionOrNull()
        if (error != null) {
            @OptIn(ExperimentalAtomicApi::class)
            throwableReference.compareAndSet(null, error)
            return
        }
        val buffer = output.getOrThrow()
        withContext(ioContext) {
            buffer.transferTo(sink)
        }
        writtenFrameNotifications.send(Unit)
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
}
