package io.github.shaksternano.gifcodec

import kotlinx.atomicfu.AtomicInt
import kotlinx.atomicfu.atomic
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.io.Buffer
import kotlinx.io.Sink
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
    maxBufferedFrames: Int = 2,
    scope: CoroutineScope = CoroutineScope(EmptyCoroutineContext),
    /**
     * Wraps IO operations so that they can be suspending instead of blocking.
     * Warning: setting this incorrectly can cause deadlocks.
     */
    private val wrapIo: suspend (() -> Unit) -> Unit = {
        withContext(Dispatchers.IO) {
            it()
        }
    },
    private val onFrameProcessed: suspend (index: Int) -> Unit = {},
) : SuspendClosable {

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

    private val quantizeExecutor: SequentialParallelExecutor<QuantizeInput, QuantizeOutput> =
        SequentialParallelExecutor(
            bufferSize = maxBufferedFrames,
            scope = scope,
            task = ::quantizeImage,
            onOutput = ::writeOrOptimizeGifImage,
        )

    private val encodeExecutor: SequentialParallelExecutor<EncodeInput, Buffer> =
        SequentialParallelExecutor(
            bufferSize = maxBufferedFrames,
            scope = scope,
            task = ::encodeGifImage,
            onOutput = ::queueWrite,
        )

    private val writeChannel: Channel<Buffer> = Channel(maxBufferedFrames)

    private val processedFrameIndex: AtomicInt = atomic(0)

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
            quantizeAndWriteFrame = { optimizedImage, originalImage, durationCentiseconds, disposalMethod ->
                quantizeAndWriteFrame(optimizedImage, originalImage, durationCentiseconds, disposalMethod)
            },
            wrapIo = {
                wrapIo(it)
            },
        )
        if (!willBeWritten) {
            onFrameProcessed()
        }
    }

    private suspend fun quantizeAndWriteFrame(
        optimizedImage: Image,
        originalImage: Image,
        durationCentiseconds: Int,
        disposalMethod: DisposalMethod,
    ) = coroutineScope {
        val submitJob = launch {
            quantizeExecutor.submit(
                QuantizeInput(
                    optimizedImage,
                    originalImage,
                    durationCentiseconds,
                    disposalMethod,
                )
            )
        }
        val flushJob = launch {
            flushRemaining()
        }
        submitJob.join()
        flushJob.cancel()
    }

    private fun quantizeImage(input: QuantizeInput): QuantizeOutput {
        val (
            image,
            originalImage,
            durationCentiseconds,
            disposalMethod,
        ) = input
        return QuantizeOutput(
            baseEncoder.getImageData(image),
            originalImage,
            durationCentiseconds,
            disposalMethod,
        )
    }

    private suspend fun writeOrOptimizeGifImage(output: QuantizeOutput) {
        val (
            imageData,
            originalImage,
            durationCentiseconds,
            disposalMethod,
        ) = output
        baseEncoder.writeOrOptimizeGifImage(
            imageData,
            originalImage,
            durationCentiseconds,
            disposalMethod,
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

    private suspend fun queueWrite(buffer: Buffer) {
        writeChannel.send(buffer)
        onFrameProcessed()
    }

    private suspend fun transferToSink(buffer: Buffer) {
        wrapIo {
            buffer.transferTo(sink)
        }
    }

    private suspend fun flushRemaining() {
        for (buffer in writeChannel) {
            transferToSink(buffer)
        }
    }

    private suspend fun onFrameProcessed() {
        val index = processedFrameIndex.getAndIncrement()
        onFrameProcessed(index)
    }

    override suspend fun close() {
        baseEncoder.close(
            quantizeAndWriteFrame = { optimizedImage, originalImage, durationCentiseconds, disposalMethod ->
                quantizeAndWriteFrame(optimizedImage, originalImage, durationCentiseconds, disposalMethod)
            },
            encodeAndWriteImage = { imageData, durationCentiseconds, disposalMethod ->
                encodeAndWriteImage(imageData, durationCentiseconds, disposalMethod)
            },
            wrapIo = {
                wrapIo(it)
            },
            finalize = {
                coroutineScope {
                    launch {
                        quantizeExecutor.close()
                        encodeExecutor.close()
                        writeChannel.close()
                    }
                    flushRemaining()
                }
            },
        )
    }

    private data class QuantizeInput(
        val optimizedImage: Image,
        val originalImage: Image,
        val durationCentiseconds: Int,
        val disposalMethod: DisposalMethod,
    )

    private data class QuantizeOutput(
        val data: QuantizedImageData,
        val originalImage: Image,
        val durationCentiseconds: Int,
        val disposalMethod: DisposalMethod,
    )

    private data class EncodeInput(
        val imageData: QuantizedImageData,
        val durationCentiseconds: Int,
        val disposalMethod: DisposalMethod,
    )
}
