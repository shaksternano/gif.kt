package com.shakster.gifkt.internal

import com.shakster.gifkt.ColorQuantizer
import com.shakster.gifkt.ColorTable

internal data class NeuQuantizer(
    private val samplingFactor: Int,
) : ColorQuantizer {

    override fun quantize(rgb: ByteArray, maxColors: Int): ColorTable {
        return NeuQuantColorTable(NeuQuant(rgb, maxColors, samplingFactor))
    }

    private class NeuQuantColorTable(
        private val neuQuant: NeuQuant,
    ) : ColorTable {

        override val colors: ByteArray = neuQuant.process()

        override fun getColorIndex(red: Int, green: Int, blue: Int): Int {
            return neuQuant.map(red, green, blue)
        }

        override fun toString(): String {
            return "NeuQuantColorTable(colors=${colors.contentToString()})"
        }
    }

    companion object {
        internal const val NEU_QUANT_MAX_SAMPLING_FACTOR: Int = 30
    }
}
