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
        private val colorIndices: IntIntMap = IntIntMap()

        init {
            val distinctColors = IntSet()
            for (i in rgb.indices step 3) {
                val red = rgb[i].toInt()
                val green = rgb[i + 1].toInt()
                val blue = rgb[i + 2].toInt()
                distinctColors.add(RGB(red, green, blue).value)
            }
            colors = ByteArray(distinctColors.size * 3)
            distinctColors.forEachIndexed { i, color ->
                val index = i * 3
                val rgb = RGB(color)
                colors[index] = rgb.red.toByte()
                colors[index + 1] = rgb.green.toByte()
                colors[index + 2] = rgb.blue.toByte()
                colorIndices[color] = i
            }
        }

        override fun getColorIndex(red: Int, green: Int, blue: Int): Int {
            val color = RGB(red, green, blue)
            return try {
                colorIndices[color.value]
            } catch (t: Throwable) {
                throw IllegalArgumentException("Color $color not found", t)
            }
        }
    }
}
