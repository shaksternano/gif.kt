package com.shakster.gifkt.internal

import kotlinx.io.Buffer
import kotlinx.io.RawSource

internal class ByteArraySource(
    private val bytes: ByteArray,
    private var offset: Int = 0,
) : RawSource {

    init {
        require(offset >= 0) { "offset ($offset) < 0" }
        require(offset <= bytes.size) { "offset ($offset) > bytes.size (${bytes.size})" }
    }

    override fun readAtMostTo(sink: Buffer, byteCount: Long): Long {
        if (offset >= bytes.size) return -1
        require(byteCount >= 0) { "byteCount ($byteCount) < 0" }
        val toRead = minOf(byteCount.toInt(), bytes.size - offset)
        sink.write(bytes, offset, offset + toRead)
        offset += toRead
        return toRead.toLong()
    }

    override fun close() = Unit
}
