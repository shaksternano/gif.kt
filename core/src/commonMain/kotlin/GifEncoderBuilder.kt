package com.shakster.gifkt

import kotlinx.coroutines.CoroutineScope
import kotlinx.io.Sink
import kotlin.coroutines.CoroutineContext
import kotlin.time.Duration

expect class GifEncoderBuilder(
    sink: Sink,
) {

    var colorDifferenceTolerance: Double
    var quantizedColorDifferenceTolerance: Double
    var loopCount: Int
    var maxColors: Int
    var colorQuantizer: ColorQuantizer
    var colorSimilarityChecker: ColorSimilarityChecker
    var comment: String
    var alphaFill: Int
    var cropTransparent: Boolean
    var minimumFrameDurationCentiseconds: Int
    var maxConcurrency: Int
    var coroutineScope: CoroutineScope
    var ioContext: CoroutineContext

    fun build(
        onFrameWritten: (
            framesWritten: Int,
            writtenDuration: Duration,
        ) -> Unit = { _, _ -> },
    ): GifEncoder
}
