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
