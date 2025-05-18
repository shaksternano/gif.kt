package io.github.shaksternano.gifcodec

import io.github.shaksternano.gifcodec.internal.MonitoredSource
import io.github.shaksternano.gifcodec.internal.readLzwIndexStream
import io.github.shaksternano.gifcodec.internal.writeByte
import kotlinx.io.Buffer
import kotlin.test.Test
import kotlin.test.assertContentEquals

class TestLzwDecoder {

    @Test
    fun testLzwDecode() {
        /*
         * Image and index stream data from:
         * https://www.matthewflickinger.com/lab/whatsinagif/lzw_image_data.asp
         */
        val buffer = Buffer()
        val imageData = listOf(
            0x02, 0x16, 0x8C, 0x2D,
            0x99, 0x87, 0x2A, 0x1C,
            0xDC, 0x33, 0xA0, 0x02,
            0x75, 0xEC, 0x95, 0xFA,
            0xA8, 0xDE, 0x60, 0x8C,
            0x04, 0x91, 0x4C, 0x01,
            0x00,
        )
        imageData.forEach {
            buffer.writeByte(it)
        }
        val indexStream = MonitoredSource(buffer).readLzwIndexStream().toByteArray()
        val expectedIndices = byteArrayOf(
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
        assertContentEquals(expectedIndices, indexStream)
    }
}
