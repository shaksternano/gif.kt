package com.shakster.gifkt

import com.shakster.gifkt.internal.NeuQuantizer
import com.shakster.gifkt.internal.OctreeQuantizer
import kotlin.jvm.JvmField
import kotlin.jvm.JvmStatic

fun interface ColorQuantizer {

    fun quantize(rgb: ByteArray, maxColors: Int): ColorTable

    companion object {
        @JvmField
        val NEU_QUANT: ColorQuantizer = neuQuant(10)

        @JvmField
        val NEU_QUANT_MAX_QUALITY: ColorQuantizer = neuQuant(1)

        @JvmField
        val NEU_QUANT_MIN_QUALITY: ColorQuantizer = neuQuant(NeuQuantizer.NEU_QUANT_MAX_SAMPLING_FACTOR)

        @JvmField
        val OCTREE: ColorQuantizer = OctreeQuantizer

        @JvmStatic
        fun neuQuant(samplingFactor: Int): ColorQuantizer {
            return NeuQuantizer(samplingFactor.coerceIn(1, NeuQuantizer.NEU_QUANT_MAX_SAMPLING_FACTOR))
        }
    }
}
