package com.shakster.gifkt

import kotlin.jvm.JvmField

fun interface ColorQuantizer {

    fun quantize(rgb: ByteArray, maxColors: Int): ColorTable

    companion object {
        @JvmField
        val NEU_QUANT: ColorQuantizer = NeuQuantizer(10)

        @JvmField
        val NEU_QUANT_MAX_QUALITY: ColorQuantizer = NeuQuantizer(NeuQuantizer.NEU_QUANT_MAX_SAMPLING_FACTOR)

        @JvmField
        val NEU_QUANT_MIN_QUALITY: ColorQuantizer = NeuQuantizer(1)
    }
}
