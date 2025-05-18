package io.github.shaksternano.gifcodec.internal

import kotlinx.io.Buffer
import kotlinx.io.RawSource

internal fun ByteArray.asSource(offset: Int = 0): RawSource = ByteArraySource(this, offset)

internal class ByteArraySource(
    private val bytes: ByteArray,
    offset: Int = 0,
) : RawSource {

    init {
        require(offset >= 0) { "offset ($offset) < 0" }
        require(offset <= bytes.size) { "offset ($offset) > bytes.size (${bytes.size})" }
    }

    private var bytesRead: Int = offset

    override fun readAtMostTo(sink: Buffer, byteCount: Long): Long {
        if (bytesRead >= bytes.size) return -1
        require(byteCount >= 0) { "byteCount ($byteCount) < 0" }
        val toRead = minOf(byteCount.toInt(), bytes.size - bytesRead)
        sink.write(bytes, bytesRead, bytesRead + toRead)
        bytesRead += toRead
        return toRead.toLong()
    }

    override fun close() = Unit
}
