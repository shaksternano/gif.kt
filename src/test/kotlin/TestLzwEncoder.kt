package io.github.shaksternano.gifcodec

import kotlinx.io.Buffer
import kotlinx.io.readByteArray
import kotlin.test.Test
import kotlin.test.assertEquals

class TestLzwEncoder {

    @Test
    fun testLzwEncode() {
        /*
         * Image and index stream data from:
         * https://www.matthewflickinger.com/lab/whatsinagif/lzw_image_data.asp
         */
        val indexStream = listOf<Byte>(
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
        val buffer = Buffer()
        buffer.writeLzwIndexStream(indexStream, maxColors)
        val imageData = buffer.readByteArray()
            .asList()
            .map {
                it.toInt() and 0xFF
            }
            .asHexByteList()
        val expectedImageData = HexByteList(
            0x02, 0x16, 0x8C, 0x2D,
            0x99, 0x87, 0x2A, 0x1C,
            0xDC, 0x33, 0xA0, 0x02,
            0x75, 0xEC, 0x95, 0xFA,
            0xA8, 0xDE, 0x60, 0x8C,
            0x04, 0x91, 0x4C, 0x01,
            0x00,
        )
        assertEquals(expectedImageData, imageData)
    }
}
