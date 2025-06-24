package com.shakster.gifkt

import kotlinx.coroutines.CoroutineScope
import kotlinx.io.Sink
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.time.Duration

actual class GifEncoderBuilder actual constructor(
    internal val sink: Sink,
) {

    actual var transparencyColorTolerance: Double = 0.0

    actual var quantizedTransparencyColorTolerance: Double = -1.0

    actual var loopCount: Int = 0

    actual var maxColors: Int = GIF_MAX_COLORS

    actual var colorQuantizer: ColorQuantizer = ColorQuantizer.DEFAULT

    actual var colorDistanceCalculator: ColorDistanceCalculator = ColorDistanceCalculator.DEFAULT

    actual var comment: String = ""

    actual var alphaFill: Int = -1

    actual var cropTransparent: Boolean = true

    actual var minimumFrameDurationCentiseconds: Int = GIF_MINIMUM_FRAME_DURATION_CENTISECONDS

    actual var maxConcurrency: Int = 2

    actual var coroutineScope: CoroutineScope = CoroutineScope(EmptyCoroutineContext)

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
}
