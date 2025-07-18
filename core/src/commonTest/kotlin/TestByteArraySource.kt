package com.shakster.gifkt

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
        val source = bytes.source()
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
        val source = bytes.source(offset)
        val result = ByteArray(arraySize - offset)
        source.readTo(result)
        val expected = bytes.copyOfRange(offset, arraySize)
        assertContentEquals(expected, result)
        assertTrue(source.exhausted())
    }
}
