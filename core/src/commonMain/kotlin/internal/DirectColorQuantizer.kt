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
            val distinctColors = rgb.asList()
                .chunked(3)
                .toSet()
            colors = distinctColors.flatten()
                .toByteArray()
            colorIndices = distinctColors.mapIndexed { i, (red, green, blue) ->
                RGB(
                    red.toInt(),
                    green.toInt(),
                    blue.toInt(),
                ) to i
            }.toMap()
        }

        override fun getColorIndex(red: Int, green: Int, blue: Int): Int {
            val color = RGB(red, green, blue)
            return colorIndices.getOrElse(color) {
                throw IllegalArgumentException("Color $color not found")
            }
        }
    }
}
