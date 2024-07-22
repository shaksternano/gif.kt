package io.github.shaksternano.gifcodec

fun interface ColorQuantizer {

    fun quantize(rgb: ByteArray, maxColors: Int): ColorTable
}
