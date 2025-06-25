package com.shakster.gifkt

import kotlinx.coroutines.CoroutineScope
import kotlinx.io.Sink
import kotlin.coroutines.CoroutineContext
import kotlin.time.Duration

expect class GifEncoderBuilder(
    sink: Sink,
) {

    var transparencyColorTolerance: Double
    var quantizedTransparencyColorTolerance: Double
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

    fun transparencyColorTolerance(colorTolerance: Double): GifEncoderBuilder

    fun quantizedTransparencyColorTolerance(colorTolerance: Double): GifEncoderBuilder

    fun loopCount(loopCount: Int): GifEncoderBuilder

    fun maxColors(maxColors: Int): GifEncoderBuilder

    fun colorQuantizer(colorQuantizer: ColorQuantizer): GifEncoderBuilder

    fun colorSimilarityChecker(colorSimilarityChecker: ColorSimilarityChecker): GifEncoderBuilder

    fun comment(comment: String): GifEncoderBuilder

    fun alphaFill(alphaFill: Int): GifEncoderBuilder

    fun cropTransparent(cropTransparent: Boolean): GifEncoderBuilder

    fun minimumFrameDurationCentiseconds(durationCentiseconds: Int): GifEncoderBuilder

    fun maxConcurrency(maxConcurrency: Int): GifEncoderBuilder

    fun coroutineScope(coroutineScope: CoroutineScope): GifEncoderBuilder

    fun ioContext(ioContext: CoroutineContext): GifEncoderBuilder

    fun build(
        onFrameWritten: (
            framesWritten: Int,
            writtenDuration: Duration,
        ) -> Unit = { _, _ -> },
    ): GifEncoder
}
