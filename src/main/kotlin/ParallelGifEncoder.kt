package io.github.shaksternano.gifcodec

import kotlinx.coroutines.CoroutineScope
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
    private val wrapIo: suspend (() -> Unit) -> Unit = { it() },
) : SuspendClosable, CoroutineScope by scope {

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
            task = ::quantizeImage,
            onOutput = ::writeOrOptimizeGifImage,
            scope = scope,
        )

    private val encodeExecutor: SequentialParallelExecutor<EncodeInput, Buffer> =
        SequentialParallelExecutor(
            bufferSize = maxBufferedFrames,
            task = ::encodeGifImage,
            onOutput = ::writeGifImage,
            scope = scope,
        )

    suspend fun writeFrame(
        image: IntArray,
        width: Int,
        height: Int,
        duration: Duration,
    ) {
        baseEncoder.writeFrame(
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
    }

    private suspend fun quantizeAndWriteFrame(
        optimizedImage: Image,
        originalImage: Image,
        durationCentiseconds: Int,
        disposalMethod: DisposalMethod,
    ) {
        quantizeExecutor.submit(
            QuantizeInput(
                optimizedImage,
                originalImage,
                durationCentiseconds,
                disposalMethod,
            )
        )
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

    private suspend fun writeGifImage(buffer: Buffer) {
        wrapIo {
            buffer.transferTo(sink)
        }
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
                quantizeExecutor.close()
                encodeExecutor.close()
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
