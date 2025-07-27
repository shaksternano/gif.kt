package com.shakster.gifkt

import kotlinx.coroutines.CoroutineScope
import kotlinx.io.Sink
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.time.Duration

actual class GifEncoderBuilder actual constructor(
    internal val sink: Sink,
) {

    actual var colorDifferenceTolerance: Double = 0.0

    actual var quantizedColorDifferenceTolerance: Double = -1.0

    actual var loopCount: Int = 0

    actual var maxColors: Int = GIF_MAX_COLORS

    actual var colorQuantizer: ColorQuantizer = ColorQuantizer.NEU_QUANT

    actual var colorSimilarityChecker: ColorSimilarityChecker = ColorSimilarityChecker.EUCLIDEAN_LUMINANCE_WEIGHTING

    actual var comment: String = ""

    actual var alphaFill: Int = -1

    actual var cropTransparent: Boolean = true

    actual var minimumFrameDurationCentiseconds: Int = GIF_MINIMUM_FRAME_DURATION_CENTISECONDS

    actual var maxConcurrency: Int = 2

    actual var coroutineScope: CoroutineScope = CoroutineScope(EmptyCoroutineContext)

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
}
