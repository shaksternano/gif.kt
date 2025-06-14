package com.shakster.gifkt.internal

import com.shakster.gifkt.ColorQuantizer
import com.shakster.gifkt.ColorTable

internal data object DirectColorQuantizer : ColorQuantizer {

    override fun quantize(rgb: ByteArray, maxColors: Int): ColorTable =
        DirectColorTable(rgb)

    private class DirectColorTable(
        rgb: ByteArray,
    ) : ColorTable {

        override val colors: ByteArray
        private val colorIndices: Map<List<Byte>, Int>

        init {
            val distinctColors = rgb.asList()
                .chunked(3)
                .distinct()
            colors = distinctColors.flatten()
                .toByteArray()
            colorIndices = distinctColors.mapIndexed { i, color ->
                color to i
            }.toMap()
        }

        override fun getColorIndex(red: Int, green: Int, blue: Int): Int {
            val colorParts = listOf(red.toByte(), green.toByte(), blue.toByte())
            return colorIndices.getOrElse(colorParts) {
                val color = red shl 16 or (green shl 8) or blue
                throw IllegalArgumentException("Color ${color.toString(16)} not found")
            }
        }
    }
}
