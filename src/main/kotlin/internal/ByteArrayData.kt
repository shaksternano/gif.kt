package io.github.shaksternano.gifcodec.internal

import io.github.shaksternano.gifcodec.RandomAccessData
import kotlinx.io.RawSource

internal class ByteArrayData(
    private val data: ByteArray,
) : RandomAccessData {

    override fun read(offset: Long): RawSource {
        return data.asSource(offset.toInt())
    }

    override fun close() = Unit
}
