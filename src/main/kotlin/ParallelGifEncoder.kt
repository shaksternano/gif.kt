package io.github.shaksternano.gifcodec

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.io.Buffer
import kotlinx.io.Sink
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.time.Duration

class ParallelGifEncoder(
    sink: Sink,
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
        quantizeInput.send(
            QuantizeInput(
                optimizedImage,
                originalImage,
                durationCentiseconds,
                disposalMethod,
            )
        )
    }

    private val quantizeInput: Channel<QuantizeInput> = Channel(maxBufferedFrames)
    private val quantizeOutput: Channel<QuantizeOutput> = Channel(maxBufferedFrames)

    private val quantizeJob: Job = launch {
        var index = 0
        for (parameters in quantizeInput) {
            val finalIndex = index
            launch {
                val (
                    image,
                    originalImage,
                    durationCentiseconds,
                    disposalMethod,
                ) = parameters
                val result = QuantizeOutput(
                    finalIndex,
                    baseEncoder.getImageData(image),
                    originalImage,
                    durationCentiseconds,
                    disposalMethod,
                )
                quantizeOutput.send(result)
            }
            index++
        }
    }

    private val receiveQuantizeOutputJob: Job = launch {
        quantizeOutput.forEachSorted(QuantizeOutput::index) {
            val (
                _,
                imageData,
                originalImage,
                durationCentiseconds,
                disposalMethod,
            ) = it
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
    }

    private val encodeInput: Channel<EncodeInput> = Channel(maxBufferedFrames)
    private val encodeOutput: Channel<Pair<Int, Buffer>> = Channel(maxBufferedFrames)

    private suspend fun encodeAndWriteImage(
        imageData: QuantizedImageData,
        durationCentiseconds: Int,
        disposalMethod: DisposalMethod,
    ) {
        encodeInput.send(
            EncodeInput(
                imageData,
                durationCentiseconds,
                disposalMethod,
            )
        )
    }

    private val encodeJob: Job = launch {
        var index = 0
        for ((imageData, durationCentiseconds, disposalMethod) in encodeInput) {
            val finalIndex = index
            launch {
                val buffer = Buffer()
                buffer.writeGifImage(
                    imageData,
                    durationCentiseconds,
                    disposalMethod,
                )
                encodeOutput.send(finalIndex to buffer)
            }
            index++
        }
    }

    private val writeDataJob: Job = launch {
        encodeOutput.forEachSorted(Pair<Int, Buffer>::first) { (_, buffer) ->
            wrapIo {
                buffer.transferTo(sink)
            }
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
                quantizeInput.close()
                quantizeJob.join()
                quantizeOutput.close()
                receiveQuantizeOutputJob.join()
                encodeInput.close()
                encodeJob.join()
                encodeOutput.close()
                writeDataJob.join()
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
        val index: Int,
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
