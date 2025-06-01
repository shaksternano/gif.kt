package com.shakster.gifkt

import com.shakster.gifkt.internal.ByteArrayData
import com.shakster.gifkt.internal.FileData
import kotlinx.io.RawSource
import kotlinx.io.files.Path
import okio.NodeJsFileSystem

private val IS_NODE_JS: Boolean = js("typeof process !== 'undefined' && process.versions && !!process.versions.node")

actual interface RandomAccessData : AutoCloseable {

    @Suppress("ACTUAL_ANNOTATIONS_NOT_MATCH_EXPECT")
    actual fun source(offset: Long): RawSource

    actual companion object {
        fun of(path: Path): RandomAccessData {
            if (IS_NODE_JS) {
                return FileData(path, NodeJsFileSystem)
            } else {
                throw UnsupportedOperationException("File access is not supported in a non-Node.js environment")
            }
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
