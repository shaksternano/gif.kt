package com.shakster.gifkt.internal

import com.shakster.gifkt.RandomAccessData
import com.shakster.gifkt.source
import kotlinx.io.Source

internal class ByteArrayData(
    private val bytes: ByteArray,
) : RandomAccessData {

    override fun source(offset: Long): Source {
        return bytes.source(offset.toInt())
    }

    override fun close() = Unit

    override fun toString(): String {
        return "ByteArrayData(data=ByteArray(size=${bytes.size}))"
    }
}
