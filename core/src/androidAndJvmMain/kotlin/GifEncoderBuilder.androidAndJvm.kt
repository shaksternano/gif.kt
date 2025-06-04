package com.shakster.gifkt

import com.shakster.gifkt.internal.GIF_MAX_COLORS
import com.shakster.gifkt.internal.GIF_MINIMUM_FRAME_DURATION_CENTISECONDS
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.future.await
import kotlinx.io.Sink
import java.util.concurrent.CompletionStage
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.time.Duration

actual class GifEncoderBuilder actual constructor(
    private val sink: Sink,
) {

    // Hide fields from Java
    @JvmSynthetic
    @JvmField
    actual var transparencyColorTolerance: Double = 0.0

    @JvmSynthetic
    @JvmField
    actual var quantizedTransparencyColorTolerance: Double = -1.0

    @JvmSynthetic
    @JvmField
    actual var loopCount: Int = 0

    @JvmSynthetic
    @JvmField
    actual var maxColors: Int = GIF_MAX_COLORS

    @JvmSynthetic
    @JvmField
    actual var colorQuantizer: ColorQuantizer = NeuQuantizer.DEFAULT

    @JvmSynthetic
    @JvmField
    actual var colorDistanceCalculator: ColorDistanceCalculator = CieLabDistanceCalculator

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

    actual fun transparencyColorTolerance(colorTolerance: Double): GifEncoderBuilder {
        this.transparencyColorTolerance = colorTolerance
        return this
    }

    actual fun quantizedTransparencyColorTolerance(colorTolerance: Double): GifEncoderBuilder {
        this.quantizedTransparencyColorTolerance = colorTolerance
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

    actual fun colorDistanceCalculator(colorDistanceCalculator: ColorDistanceCalculator): GifEncoderBuilder {
        this.colorDistanceCalculator = colorDistanceCalculator
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
            onFrameWritten,
        )
    }

    @JvmOverloads
    actual fun buildParallel(
        onFrameWritten: suspend (
            framesWritten: Int,
            writtenDuration: Duration,
        ) -> Unit,
    ): ParallelGifEncoder {
        return ParallelGifEncoder(
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
            maxConcurrency,
            coroutineScope,
            ioContext,
            onFrameWritten,
        )
    }

    fun buildParallelSyncCallback(
        onFrameWritten: (
            framesWritten: Int,
            writtenDuration: Duration,
        ) -> Unit,
    ): ParallelGifEncoder {
        return buildParallel(onFrameWritten)
    }

    fun buildParallelFutureCallback(
        onFrameWritten: (
            framesWritten: Int,
            writtenDuration: Duration,
        ) -> CompletionStage<Void>,
    ): ParallelGifEncoder {
        return buildParallel { framesWritten, writtenDuration ->
            onFrameWritten(framesWritten, writtenDuration).await()
        }
    }
}
