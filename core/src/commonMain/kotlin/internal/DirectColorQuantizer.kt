package com.shakster.gifkt.internal

import com.shakster.gifkt.ColorQuantizer
import com.shakster.gifkt.ColorTable
import com.shakster.gifkt.RGB

internal data object DirectColorQuantizer : ColorQuantizer {

    override fun quantize(rgb: ByteArray, maxColors: Int): ColorTable =
        DirectColorTable(rgb)

    private class DirectColorTable(
        rgb: ByteArray,
    ) : ColorTable {

        override val colors: ByteArray
        private val colorIndices: Map<RGB, Int>

        init {
            val distinctColors = hashSetOf<RGB>()
            for (i in rgb.indices step 3) {
                val red = rgb[i].toInt()
                val green = rgb[i + 1].toInt()
                val blue = rgb[i + 2].toInt()
                distinctColors.add(RGB(red, green, blue))
            }
            colors = ByteArray(distinctColors.size * 3)
            colorIndices = hashMapOf()
            distinctColors.forEachIndexed { i, color ->
                val index = i * 3
                colors[index] = color.red.toByte()
                colors[index + 1] = color.green.toByte()
                colors[index + 2] = color.blue.toByte()
                colorIndices[color] = i
            }
        }

        override fun getColorIndex(red: Int, green: Int, blue: Int): Int {
            val color = RGB(red, green, blue)
            return colorIndices.getOrElse(color) {
                throw IllegalArgumentException("Color $color not found")
            }
        }
    }
}
