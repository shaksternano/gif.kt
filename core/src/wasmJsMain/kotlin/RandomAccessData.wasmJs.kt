package com.shakster.gifkt

import com.shakster.gifkt.internal.ByteArrayData
import kotlinx.io.RawSource

actual interface RandomAccessData : AutoCloseable {

    @Suppress("ACTUAL_ANNOTATIONS_NOT_MATCH_EXPECT")
    actual fun source(offset: Long): RawSource

    actual companion object {
        actual fun of(byteArray: ByteArray): RandomAccessData {
            return ByteArrayData(byteArray)
        }
    }
}
