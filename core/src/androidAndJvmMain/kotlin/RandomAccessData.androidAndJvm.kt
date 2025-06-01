@file:JvmName("RandomAccessDataJvm")

package com.shakster.gifkt

import com.shakster.gifkt.internal.ByteArrayData
import com.shakster.gifkt.internal.FileData
import kotlinx.io.IOException
import kotlinx.io.RawSource
import kotlinx.io.files.Path
import okio.FileSystem
import java.io.File
import java.nio.file.Path as JavaPath

actual interface RandomAccessData : AutoCloseable {

    @Throws(IOException::class)
    actual fun source(offset: Long): RawSource

    actual companion object {
        @JvmStatic
        @Throws(IOException::class)
        fun of(path: Path): RandomAccessData {
            return FileData(path, FileSystem.SYSTEM)
        }

        @JvmStatic
        @Throws(IOException::class)
        fun of(path: JavaPath): RandomAccessData {
            return FileData(path.toString(), FileSystem.SYSTEM)
        }

        @JvmStatic
        @Throws(IOException::class)
        fun of(file: File): RandomAccessData {
            return FileData(file.toString(), FileSystem.SYSTEM)
        }

        @JvmStatic
        actual fun of(byteArray: ByteArray): RandomAccessData {
            return ByteArrayData(byteArray)
        }
    }
}

@Suppress("EXTENSION_SHADOWED_BY_MEMBER")
@JvmSynthetic
@Throws(IOException::class)
actual fun RandomAccessData.Companion.of(path: Path): RandomAccessData {
    return of(path)
}

@Throws(IOException::class)
fun JavaPath.asRandomAccess(): RandomAccessData {
    return RandomAccessData.of(this)
}

@Throws(IOException::class)
fun File.asRandomAccess(): RandomAccessData {
    return RandomAccessData.of(this)
}
