package com.shakster.gifkt

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.future.await
import kotlinx.io.Sink
import java.util.concurrent.CompletionStage
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.time.Duration
import kotlin.time.toJavaDuration
import java.time.Duration as JavaDuration

actual class GifEncoderBuilder actual constructor(
    private val sink: Sink,
) {

    // Hide fields from Java
    @JvmSynthetic
    @JvmField
    actual var colorDifferenceTolerance: Double = 0.0

    @JvmSynthetic
    @JvmField
    actual var quantizedColorDifferenceTolerance: Double = -1.0

    @JvmSynthetic
    @JvmField
    actual var loopCount: Int = 0

    @JvmSynthetic
    @JvmField
    actual var maxColors: Int = GIF_MAX_COLORS

    @JvmSynthetic
    @JvmField
    actual var colorQuantizer: ColorQuantizer = ColorQuantizer.DEFAULT

    @JvmSynthetic
    @JvmField
    actual var colorSimilarityChecker: ColorSimilarityChecker = ColorSimilarityChecker.DEFAULT

    @JvmSynthetic
    @JvmField
    actual var comment: String = ""

    @JvmSynthetic
    @JvmField
    actual var alphaFill: Int = -1

    @JvmSynthetic
    @JvmField
    actual var cropTransparent: Boolean = true

    @JvmSynthetic
    @JvmField
    actual var minimumFrameDurationCentiseconds: Int = GIF_MINIMUM_FRAME_DURATION_CENTISECONDS

    @JvmSynthetic
    @JvmField
    actual var maxConcurrency: Int = 2

    @JvmSynthetic
    @JvmField
    actual var coroutineScope: CoroutineScope = CoroutineScope(EmptyCoroutineContext)

    @JvmSynthetic
    @JvmField
    actual var ioContext: CoroutineContext = EmptyCoroutineContext

    actual fun colorDifferenceTolerance(colorTolerance: Double): GifEncoderBuilder {
        this.colorDifferenceTolerance = colorTolerance
        return this
    }

    actual fun quantizedColorDifferenceTolerance(colorTolerance: Double): GifEncoderBuilder {
        this.quantizedColorDifferenceTolerance = colorTolerance
        return this
    }

    actual fun loopCount(loopCount: Int): GifEncoderBuilder {
        this.loopCount = loopCount
        return this
    }

    actual fun maxColors(maxColors: Int): GifEncoderBuilder {
        this.maxColors = maxColors
        return this
    }

    actual fun colorQuantizer(colorQuantizer: ColorQuantizer): GifEncoderBuilder {
        this.colorQuantizer = colorQuantizer
        return this
    }

    actual fun colorSimilarityChecker(colorSimilarityChecker: ColorSimilarityChecker): GifEncoderBuilder {
        this.colorSimilarityChecker = colorSimilarityChecker
        return this
    }

    actual fun comment(comment: String): GifEncoderBuilder {
        this.comment = comment
        return this
    }

    actual fun alphaFill(alphaFill: Int): GifEncoderBuilder {
        this.alphaFill = alphaFill
        return this
    }

    actual fun cropTransparent(cropTransparent: Boolean): GifEncoderBuilder {
        this.cropTransparent = cropTransparent
        return this
    }

    actual fun minimumFrameDurationCentiseconds(durationCentiseconds: Int): GifEncoderBuilder {
        require(minimumFrameDurationCentiseconds > 0) {
            "Minimum frame duration must be positive: $minimumFrameDurationCentiseconds"
        }
        this.minimumFrameDurationCentiseconds = durationCentiseconds
        return this
    }

    actual fun maxConcurrency(maxConcurrency: Int): GifEncoderBuilder {
        require(maxConcurrency > 0) {
            "maxConcurrency must be positive: $maxConcurrency"
        }
        this.maxConcurrency = maxConcurrency
        return this
    }

    actual fun coroutineScope(coroutineScope: CoroutineScope): GifEncoderBuilder {
        this.coroutineScope = coroutineScope
        return this
    }

    actual fun ioContext(ioContext: CoroutineContext): GifEncoderBuilder {
        this.ioContext = ioContext
        return this
    }

    @JvmOverloads
    actual fun build(
        onFrameWritten: (
            framesWritten: Int,
            writtenDuration: Duration,
        ) -> Unit,
    ): GifEncoder {
        return GifEncoder(
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
    }

    @JvmOverloads
    fun buildParallel(
        onFrameWritten: suspend (
            framesWritten: Int,
            writtenDuration: Duration,
        ) -> Unit = { _, _ -> },
    ): ParallelGifEncoder {
        return ParallelGifEncoder(
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
            ioContext,
            onFrameWritten,
        )
    }

    fun buildParallelSyncCallback(
        callback: OnFrameWrittenCallback,
    ): ParallelGifEncoder {
        return buildParallel { framesWritten, writtenDuration ->
            callback.onFrameWritten(
                framesWritten,
                writtenDuration.toJavaDuration(),
            )
        }
    }

    fun buildParallelFutureCallback(
        onFrameWritten: (
            framesWritten: Int,
            writtenDuration: JavaDuration,
        ) -> CompletionStage<*>,
    ): ParallelGifEncoder {
        return buildParallel { framesWritten, writtenDuration ->
            onFrameWritten(
                framesWritten,
                writtenDuration.toJavaDuration(),
            ).await()
        }
    }
}

actual fun GifEncoderBuilder.buildParallel(
    onFrameWritten: suspend (
        framesWritten: Int,
        writtenDuration: Duration,
    ) -> Unit,
): ParallelGifEncoder {
    return buildParallel(onFrameWritten)
}
