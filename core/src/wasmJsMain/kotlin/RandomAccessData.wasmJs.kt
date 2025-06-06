package com.shakster.gifkt

import com.shakster.gifkt.internal.ByteArrayData
import kotlinx.io.RawSource

actual interface RandomAccessData : AutoCloseable {

    actual fun source(offset: Long): RawSource

    actual override fun close()

    actual companion object {
        actual fun of(bytes: ByteArray): RandomAccessData {
            return ByteArrayData(bytes)
        }
    }
}
