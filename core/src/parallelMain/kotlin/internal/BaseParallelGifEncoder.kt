package com.shakster.gifkt.internal

import com.shakster.gifkt.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.withContext
import kotlinx.io.Buffer
import kotlinx.io.Sink
import kotlin.coroutines.CoroutineContext
import kotlin.time.Duration

internal class BaseParallelGifEncoder(
    sink: Sink,
    colorDifferenceTolerance: Double,
    quantizedColorDifferenceTolerance: Double,
    loopCount: Int,
    maxColors: Int,
    private val colorQuantizer: ColorQuantizer,
    colorSimilarityChecker: ColorSimilarityChecker,
    comment: String,
    alphaFill: Int,
    cropTransparent: Boolean,
    minimumFrameDurationCentiseconds: Int,
    maxConcurrency: Int,
    coroutineScope: CoroutineScope,
    private val ioContext: CoroutineContext,
    private val onFrameWrittenCallback: suspend (
        framesWritten: Int,
        writtenDuration: Duration,
    ) -> Unit,
) : AsyncGifEncoder(
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
    maxConcurrency,
    coroutineScope,
) {

    override suspend fun quantizeImage(input: QuantizeInput): QuantizeOutput {
        return QuantizeOutput(
            quantizeGifImage(
                input.optimizedImage,
                maxColors,
                colorQuantizer,
                optimizeQuantizedTransparency,
            ),
            input.originalImage,
            input.durationCentiseconds,
            input.disposalMethod,
            input.optimizedPreviousFrame,
        )
    }

    override suspend fun encodeGifImage(input: EncodeInput): EncodeOutput {
        val buffer = Buffer()
        buffer.writeGifImage(
            input.imageData,
            input.durationCentiseconds,
            input.disposalMethod,
        )
        return EncodeOutput(buffer, input.durationCentiseconds.centiseconds)
    }

    override suspend fun wrapIo(block: () -> Unit) {
        withContext(ioContext) {
            block()
        }
    }

    override suspend fun onFrameWritten(framesWritten: Int, writtenDuration: Duration) {
        onFrameWrittenCallback(framesWritten, writtenDuration)
    }
}
