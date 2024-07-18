package io.github.shaksternano.gifcodec

interface ColorQuantizer {

    fun quantize(rgb: ByteArray, maxColors: Int): ColorTable
}
