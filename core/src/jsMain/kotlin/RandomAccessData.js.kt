package com.shakster.gifkt

import com.shakster.gifkt.internal.ByteArrayData
import kotlinx.io.RawSource
import org.khronos.webgl.ArrayBuffer
import org.khronos.webgl.Int8Array

actual interface RandomAccessData : AutoCloseable {

    actual fun source(offset: Long): RawSource

    actual companion object {
        actual fun of(bytes: ByteArray): RandomAccessData {
            return ByteArrayData(bytes)
        }

        fun of(buffer: ArrayBuffer): RandomAccessData {
            return ByteArrayData(Int8Array(buffer).unsafeCast<ByteArray>())
        }

        fun of(bytes: Int8Array): RandomAccessData {
            return ByteArrayData(bytes.unsafeCast<ByteArray>())
        }
    }
}

fun ArrayBuffer.asRandomAccess(): RandomAccessData {
    return RandomAccessData.of(this)
}

fun Int8Array.asRandomAccess(): RandomAccessData {
    return RandomAccessData.of(this)
}
