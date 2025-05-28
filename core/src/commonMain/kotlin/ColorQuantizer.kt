package com.shakster.gifkt

fun interface ColorQuantizer {
    fun quantize(rgb: ByteArray, maxColors: Int): ColorTable
}
