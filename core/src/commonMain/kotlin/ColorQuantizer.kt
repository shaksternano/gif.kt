package com.shakster.gifkt

import kotlin.jvm.JvmField

fun interface ColorQuantizer {

    fun quantize(rgb: ByteArray, maxColors: Int): ColorTable

    companion object {
        @JvmField
        val DEFAULT: ColorQuantizer = NeuQuantizer.DEFAULT
    }
}
