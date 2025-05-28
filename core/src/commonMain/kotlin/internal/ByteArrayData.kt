package com.shakster.gifkt.internal

import com.shakster.gifkt.RandomAccessData
import kotlinx.io.RawSource

internal class ByteArrayData(
    private val data: ByteArray,
) : RandomAccessData {

    override fun read(offset: Long): RawSource {
        return ByteArraySource(data, offset.toInt())
    }

    override fun close() = Unit
}
