package io.github.shaksternano.gifcodec

import kotlin.test.Test
import kotlin.test.assertEquals

class TestLzwEncoder {

    @Test
    fun testLzwEncode() {
        /*
         * Image and code stream data from:
         * https://www.matthewflickinger.com/lab/whatsinagif/lzw_image_data.asp
         */
        val imageColorIndices = byteArrayOf(
            1, 1, 1, 1, 1,   2, 2, 2, 2, 2,
            1, 1, 1, 1, 1,   2, 2, 2, 2, 2,
            1, 1, 1, 1, 1,   2, 2, 2, 2, 2,
            1, 1, 1, 0, 0,   0, 0, 2, 2, 2,
            1, 1, 1, 0, 0,   0, 0, 2, 2, 2,

            2, 2, 2, 0, 0,   0, 0, 1, 1, 1,
            2, 2, 2, 0, 0,   0, 0, 1, 1, 1,
            2, 2, 2, 2, 2,   1, 1, 1, 1, 1,
            2, 2, 2, 2, 2,   1, 1, 1, 1, 1,
            2, 2, 2, 2, 2,   1, 1, 1, 1, 1,
        )
        val maxColors = 4
        val codeStream = lzwEncode(imageColorIndices, maxColors)
        val expectedCodes = listOf(
            4, 1, 6, 6, 2, 9,
            9, 7, 8, 10, 2, 12,
            1, 14, 15, 6, 0, 21,
            0, 10, 7, 22, 23, 18,
            26, 7, 10, 29, 13, 24,
            12, 18, 16, 36, 12, 5,
        )
        // Code size to the number of codes that size is for
        val expectedCodeSizes = listOf(
            3 to 4,
            4 to 8,
            5 to 16,
            6 to 8,
        ).flatMap { (code, count) ->
            List(count) {
                code
            }
        }
        val expectedCodeStream = expectedCodes.zip(expectedCodeSizes)
            .map { (code, size) ->
                LzwCode(code, size)
            }
        assertEquals(expectedCodeStream, codeStream)
    }

    @Test
    fun testLargeLzwEncode() {
        val pixels = loadBgrPixels("media/restaurant.jpg")
        val neuQuant = NeuQuant(
            image = pixels,
            samplingFactor = 10,
        )
        val colorTable = neuQuant.process()
        val maxColors = colorTable.size / 3
        val imageColorIndices = pixels.asList()
            .chunked(3)
            .map { (blue, green, red) ->
                neuQuant.map(
                    blue.toInt() and 0xFF,
                    green.toInt() and 0xFF,
                    red.toInt() and 0xFF,
                ).toByte()
            }
            .toByteArray()
        val codeStream = lzwEncode(imageColorIndices, maxColors).map { it.code }
        val indexStream = lzwDecode(codeStream, maxColors)
        assertEquals(imageColorIndices.asList(), indexStream)
    }
}
