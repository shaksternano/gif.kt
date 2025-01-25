package io.github.shaksternano.gifcodec

import kotlinx.coroutines.*
import kotlinx.io.Buffer
import kotlinx.io.Sink
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
    scope: CoroutineScope = CoroutineScope(EmptyCoroutineContext),
    private val ioContext: CoroutineContext = Dispatchers.IO,
    private val onFrameProcessed: suspend (index: Int) -> Unit = {},
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

    private var processedFrameIndex: Int = 0

    private val quantizeExecutor: AsyncExecutor<QuantizeInput, QuantizeOutput> =
        AsyncExecutor(
            maxConcurrency = maxConcurrency,
            scope = scope,
            task = ::quantizeImage,
            onOutput = ::writeOrOptimizeGifImage,
        )

    private val encodeExecutor: ChannelOutputAsyncExecutor<EncodeInput, Buffer> =
        ChannelOutputAsyncExecutor(
            maxConcurrency = maxConcurrency,
            scope = scope,
            task = ::encodeGifImage,
        )

    suspend fun writeFrame(
        image: IntArray,
        width: Int,
        height: Int,
        duration: Duration,
    ) {
        val willBeWritten = baseEncoder.writeFrame(
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
        if (!willBeWritten) {
            onFrameProcessed()
        }
    }

    suspend fun writeFrame(frame: ImageFrame) =
        writeFrame(
            frame.argb,
            frame.width,
            frame.height,
            frame.duration,
        )

    // Runs on caller's thread
    private suspend fun quantizeAndWriteFrame(
        optimizedImage: Image,
        originalImage: Image,
        durationCentiseconds: Int,
        disposalMethod: DisposalMethod,
        optimizedPreviousFrame: Boolean,
    ) = coroutineScope {
        val quantizeJob = launch {
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
        while (quantizeJob.isActive) {
            flushCurrent()
        }
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
            encodeExecutor.submitFailure(error)
            return
        }
        val (
            imageData,
            originalImage,
            durationCentiseconds,
            disposalMethod,
            optimizedPreviousFrame,
        ) = output.getOrThrow()
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

    // Runs on caller's thread
    private suspend fun flushCurrent() {
        encodeExecutor.output.forEachCurrent { bufferResult ->
            transferToSink(bufferResult.getOrThrow())
        }
    }

    // Runs on caller's thread
    private suspend fun flushRemaining() {
        encodeExecutor.output.forEach { bufferResult ->
            transferToSink(bufferResult.getOrThrow())
        }
    }

    // Runs on caller's thread
    private suspend fun transferToSink(buffer: Buffer) {
        wrapIo {
            buffer.transferTo(sink)
        }
        onFrameProcessed()
    }

    // Runs on caller's thread
    private suspend fun onFrameProcessed() {
        onFrameProcessed(processedFrameIndex++)
    }

    private suspend fun wrapIo(block: () -> Unit) {
        withContext(ioContext) {
            block()
        }
    }

    override suspend fun close() = coroutineScope {
        val flushJob = launch {
            flushRemaining()
        }
        baseEncoder.close(
            quantizeAndWriteFrame = { optimizedImage, originalImage, durationCentiseconds, disposalMethod, optimizedPreviousFrame ->
                quantizeAndWriteFrame(
                    optimizedImage,
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
                quantizeExecutor.close()
            },
            afterFinalQuantizedWrite = {
                encodeExecutor.close()
                flushJob.join()
            },
            wrapIo = {
                wrapIo(it)
            },
        )
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
