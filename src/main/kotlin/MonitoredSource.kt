package io.github.shaksternano.gifcodec

import kotlinx.io.Buffer
import kotlinx.io.RawSource

class MonitoredSource(
    private val source: RawSource,
) : RawSource {

    var bytesRead: Long = 0
        private set

    override fun readAtMostTo(sink: Buffer, byteCount: Long): Long {
        val read = source.readAtMostTo(sink, byteCount)
        if (read > 0) {
            bytesRead += read
        }
        return read
    }

    override fun close() = source.close()
}
