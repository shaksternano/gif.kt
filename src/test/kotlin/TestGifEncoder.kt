package io.github.shaksternano.gifcodec

import io.github.shaksternano.gifcodec.internal.DisposalMethod
import io.github.shaksternano.gifcodec.internal.writeGifApplicationExtension
import io.github.shaksternano.gifcodec.internal.writeGifCommentExtension
import io.github.shaksternano.gifcodec.internal.writeGifGraphicsControlExtension
import io.github.shaksternano.gifcodec.internal.writeGifHeader
import io.github.shaksternano.gifcodec.internal.writeGifImageData
import io.github.shaksternano.gifcodec.internal.writeGifImageDescriptor
import io.github.shaksternano.gifcodec.internal.writeGifLogicalScreenDescriptor
import io.github.shaksternano.gifcodec.internal.writeGifTrailer
import kotlinx.io.Buffer
import kotlin.test.Test
import kotlin.test.assertContentEquals

/*
 * GIF file data from:
 * https://www.matthewflickinger.com/lab/whatsinagif/bits_and_bytes.asp
 */
class TestGifEncoder {

    @Test
    fun testWriteHeader() {
        val buffer = Buffer()
        buffer.writeGifHeader()
        val bytes = buffer.readByteList()
        val expected = HexByteList(
            0x47, 0x49, 0x46, 0x38,
            0x39, 0x61,
        )
        assertContentEquals(expected, bytes)
    }

    @Test
    fun testWriteLogicalScreenDescriptor() {
        val buffer = Buffer()
        buffer.writeGifLogicalScreenDescriptor(10, 10)
        val bytes = buffer.readByteList()
        val expected = HexByteList(
            0x0A, 0x00, 0x0A, 0x00,
            0x00, 0x00, 0x00,
        )
        assertContentEquals(expected, bytes)
    }

    @Test
    fun testWriteApplicationExtension() {
        val buffer = Buffer()
        buffer.writeGifApplicationExtension(0)
        val bytes = buffer.readByteList()
        val expected = HexByteList(
            0x21, 0xFF, 0x0B, 0x4E,
            0x45, 0x54, 0x53, 0x43,
            0x41, 0x50, 0x45, 0x32,
            0x2E, 0x30, 0x03, 0x01,
            0x00, 0x00, 0x00,
        )
        assertContentEquals(expected, bytes)
    }

    @Test
    fun testWriteCommentExtension() {
        val buffer = Buffer()
        buffer.writeGifCommentExtension(
            "File source: https://commons.wikimedia.org/wiki/File:Dancing.gif"
        )
        val bytes = buffer.readByteList()
        val expected = HexByteList(
            0x21, 0xFE, 0x40, 0x46,
            0x69, 0x6C, 0x65, 0x20,
            0x73, 0x6F, 0x75, 0x72,
            0x63, 0x65, 0x3A, 0x20,
            0x68, 0x74, 0x74, 0x70,
            0x73, 0x3A, 0x2F, 0x2F,
            0x63, 0x6F, 0x6D, 0x6D,
            0x6F, 0x6E, 0x73, 0x2E,
            0x77, 0x69, 0x6B, 0x69,
            0x6D, 0x65, 0x64, 0x69,
            0x61, 0x2E, 0x6F, 0x72,
            0x67, 0x2F, 0x77, 0x69,
            0x6B, 0x69, 0x2F, 0x46,
            0x69, 0x6C, 0x65, 0x3A,
            0x44, 0x61, 0x6E, 0x63,
            0x69, 0x6E, 0x67, 0x2E,
            0x67, 0x69, 0x66, 0x00,
        )
        assertContentEquals(expected, bytes)
    }

    @Test
    fun testWriteGraphicsControl() {
        val buffer = Buffer()
        buffer.writeGifGraphicsControlExtension(
            DisposalMethod.UNSPECIFIED,
            durationCentiseconds = 0,
            transparentColorIndex = -1,
        )
        val bytes = buffer.readByteList()
        val expected = HexByteList(
            0x21, 0xF9, 0x04, 0x00,
            0x00, 0x00, 0x00, 0x00,
        )
        assertContentEquals(expected, bytes)
    }

    @Test
    fun testWriteImageDescriptor() {
        val buffer = Buffer()
        buffer.writeGifImageDescriptor(
            width = 10,
            height = 10,
            x = 0,
            y = 0,
            localColorTableSize = 4,
        )
        val bytes = buffer.readByteList()
        val expected = HexByteList(
            0x2C, 0x00, 0x00, 0x00,
            0x00, 0x0A, 0x00, 0x0A,
            0x00, 0x81,
        )
        assertContentEquals(expected, bytes)
    }

    @Test
    fun testWriteImageData() {
        val buffer = Buffer()
        /*
         * Image data from:
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
        buffer.writeGifImageData(
            imageColorIndices,
            colorTableSize = 4,
        )
        val bytes = buffer.readByteList()
        val expected = HexByteList(
            0x02, 0x16, 0x8C, 0x2D,
            0x99, 0x87, 0x2A, 0x1C,
            0xDC, 0x33, 0xA0, 0x02,
            0x75, 0xEC, 0x95, 0xFA,
            0xA8, 0xDE, 0x60, 0x8C,
            0x04, 0x91, 0x4C, 0x01,
            0x00,
        )
        assertContentEquals(expected, bytes)
    }

    @Test
    fun testWriteTrailer() {
        val buffer = Buffer()
        buffer.writeGifTrailer()
        val bytes = buffer.readByteList()
        val expected = HexByteList(
            0x3B,
        )
        assertContentEquals(expected, bytes)
    }
}
