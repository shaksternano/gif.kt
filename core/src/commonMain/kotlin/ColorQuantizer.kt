package com.shakster.gifkt

import com.shakster.gifkt.internal.NeuQuantizer
import com.shakster.gifkt.internal.OctreeQuantizer
import kotlin.jvm.JvmField
import kotlin.jvm.JvmStatic

fun interface ColorQuantizer {

    fun quantize(rgb: ByteArray, maxColors: Int): ColorTable

    companion object {
        @JvmField
        val NEU_QUANT: ColorQuantizer = NeuQuantizer(10)

        @JvmField
        val OCTREE: ColorQuantizer = OctreeQuantizer

        // Using const fails to compile
        @Suppress("MayBeConstant")
        @JvmField
        val NEU_QUANT_MAX_SAMPLING_FACTOR: Int = 30

        @JvmStatic
        fun neuQuant(samplingFactor: Int): ColorQuantizer {
            return NeuQuantizer(samplingFactor.coerceIn(1, NEU_QUANT_MAX_SAMPLING_FACTOR))
        }
    }
}
