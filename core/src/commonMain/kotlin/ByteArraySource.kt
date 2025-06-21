package com.shakster.gifkt

import kotlinx.io.Buffer
import kotlinx.io.RawSource

fun ByteArray.source(offset: Int = 0): RawSource {
    return ByteArraySource(this, offset)
}

private class ByteArraySource(
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

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as ByteArraySource

        if (offset != other.offset) return false
        if (!bytes.contentEquals(other.bytes)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = offset
        result = 31 * result + bytes.contentHashCode()
        return result
    }

    override fun toString(): String {
        return "ByteArraySource(offset=$offset, bytes=${bytes.contentToString()})"
    }
}
