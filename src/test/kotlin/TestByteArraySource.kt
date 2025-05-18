package io.github.shaksternano.gifcodec

import io.github.shaksternano.gifcodec.internal.asSource
import kotlinx.io.buffered
import kotlinx.io.readTo
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertTrue

class TestByteArraySource {

    @Test
    fun testCorrectRead() {
        val arraySize = 100000
        val bytes = ByteArray(arraySize) { i ->
            i.toByte()
        }
        // Remove looping pattern
        bytes.shuffle(Random(0))
        val source = bytes.asSource().buffered()
        val result = ByteArray(arraySize)
        source.readTo(result)
        assertContentEquals(bytes, result)
        assertTrue(source.exhausted())
    }

    @Test
    fun testCorrectOffsetRead() {
        val arraySize = 100000
        val bytes = ByteArray(arraySize) { i ->
            i.toByte()
        }
        // Remove looping pattern
        bytes.shuffle(Random(0))
        val offset = 10000
        val source = bytes.asSource(offset).buffered()
        val result = ByteArray(arraySize - offset)
        source.readTo(result)
        val expected = bytes.copyOfRange(offset, arraySize)
        assertContentEquals(expected, result)
        assertTrue(source.exhausted())
    }
}
