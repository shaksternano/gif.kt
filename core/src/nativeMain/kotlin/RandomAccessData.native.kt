package com.shakster.gifkt

import com.shakster.gifkt.internal.ByteArrayData
import com.shakster.gifkt.internal.FileData
import kotlinx.io.IOException
import kotlinx.io.RawSource
import kotlinx.io.files.Path
import okio.FileSystem

actual interface RandomAccessData : AutoCloseable {

    @Throws(IOException::class)
    actual fun source(offset: Long): RawSource

    actual companion object {
        @Throws(IOException::class)
        fun of(path: Path): RandomAccessData {
            return FileData(path, FileSystem.SYSTEM)
        }

        actual fun of(bytes: ByteArray): RandomAccessData {
            return ByteArrayData(bytes)
        }
    }
}

@Suppress("EXTENSION_SHADOWED_BY_MEMBER")
@Throws(IOException::class)
actual fun RandomAccessData.Companion.of(path: Path): RandomAccessData {
    return of(path)
}

@Throws(IOException::class)
actual fun Path.asRandomAccess(): RandomAccessData {
    return RandomAccessData.of(this)
}
