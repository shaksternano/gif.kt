package com.shakster.gifkt

import com.shakster.gifkt.internal.ByteArrayData
import com.shakster.gifkt.internal.FileData
import kotlinx.io.RawSource
import kotlinx.io.files.Path
import okio.WasiFileSystem

actual interface RandomAccessData : AutoCloseable {

    @Suppress("ACTUAL_ANNOTATIONS_NOT_MATCH_EXPECT")
    actual fun source(offset: Long): RawSource

    actual companion object {
        fun of(path: Path): RandomAccessData {
            return FileData(path, WasiFileSystem)
        }

        actual fun of(byteArray: ByteArray): RandomAccessData {
            return ByteArrayData(byteArray)
        }
    }
}

@Suppress("ACTUAL_ANNOTATIONS_NOT_MATCH_EXPECT", "EXTENSION_SHADOWED_BY_MEMBER")
actual fun RandomAccessData.Companion.of(path: Path): RandomAccessData {
    return of(path)
}
