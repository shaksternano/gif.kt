package com.shakster.gifkt

import com.shakster.gifkt.internal.ByteArrayData
import com.shakster.gifkt.internal.FileData
import kotlinx.io.RawSource
import kotlinx.io.files.Path
import okio.NodeJsFileSystem
import org.khronos.webgl.ArrayBuffer
import org.khronos.webgl.Int8Array

private val IS_NODE_JS: Boolean = js("typeof process !== 'undefined' && process.versions && !!process.versions.node")

actual interface RandomAccessData : AutoCloseable {

    actual fun source(offset: Long): RawSource

    actual companion object {
        fun of(path: Path): RandomAccessData {
            if (IS_NODE_JS) {
                return FileData(path, NodeJsFileSystem)
            } else {
                throw UnsupportedOperationException("File access is not supported in a non-Node.js environment")
            }
        }

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

@Suppress("EXTENSION_SHADOWED_BY_MEMBER")
actual fun RandomAccessData.Companion.of(path: Path): RandomAccessData {
    return of(path)
}

actual fun Path.asRandomAccess(): RandomAccessData {
    return RandomAccessData.of(this)
}

fun ArrayBuffer.asRandomAccess(): RandomAccessData {
    return RandomAccessData.of(this)
}

fun Int8Array.asRandomAccess(): RandomAccessData {
    return RandomAccessData.of(this)
}
