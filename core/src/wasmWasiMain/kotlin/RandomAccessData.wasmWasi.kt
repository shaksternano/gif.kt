package com.shakster.gifkt

import com.shakster.gifkt.internal.ByteArrayData
import com.shakster.gifkt.internal.FileData
import kotlinx.io.RawSource
import kotlinx.io.files.Path
import okio.WasiFileSystem

actual interface RandomAccessData : AutoCloseable {

    actual fun source(offset: Long): RawSource

    actual override fun close()

    actual companion object {
        fun of(path: Path): RandomAccessData {
            return FileData(path, WasiFileSystem)
        }

        actual fun of(bytes: ByteArray): RandomAccessData {
            return ByteArrayData(bytes)
        }
    }
}

actual fun RandomAccessData.Companion.of(path: Path): RandomAccessData {
    return of(path)
}
