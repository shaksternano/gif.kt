package com.shakster.gifkt.internal

import com.shakster.gifkt.RandomAccessData
import kotlinx.io.RawSource

internal class ByteArrayData(
    private val bytes: ByteArray,
) : RandomAccessData {

    override fun source(offset: Long): RawSource {
        return ByteArraySource(bytes, offset.toInt())
    }

    override fun close() = Unit

    override fun toString(): String {
        return "ByteArrayData(data=ByteArray(size=${bytes.size}))"
    }
}
