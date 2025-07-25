package com.shakster.gifkt.internal

import kotlinx.io.Source
import kotlinx.io.readByteArray
import kotlinx.io.readShortLe
import kotlinx.io.readString

internal fun Source.monitored(): MonitoredSource = MonitoredSource(this)

internal class MonitoredSource(
    private val source: Source,
) : AutoCloseable {

    var bytesRead: Long = 0
        private set

    fun readByte(): Byte {
        val byte = source.readByte()
        bytesRead++
        return byte
    }

    fun readLittleEndianShort(): Int {
        val short = source.readShortLe()
        bytesRead += 2
        return short.toInt()
    }

    fun readByteArray(byteCount: Int): ByteArray {
        val bytes = source.readByteArray(byteCount)
        bytesRead += byteCount.toLong()
        return bytes
    }

    fun skip(byteCount: Long) {
        source.skip(byteCount)
        bytesRead += byteCount
    }

    fun readString(byteCount: Long): String {
        val string = source.readString(byteCount)
        bytesRead += byteCount
        return string
    }

    fun exhausted(): Boolean = source.exhausted()

    override fun close() = source.close()
}
