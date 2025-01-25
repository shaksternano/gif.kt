package io.github.shaksternano.gifcodec

import kotlinx.io.Buffer
import kotlinx.io.RawSource
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import okio.FileHandle
import okio.FileSystem
import okio.Path.Companion.toPath
import okio.buffer

interface RandomAccessData : AutoCloseable {
    fun read(offset: Long = 0): RawSource
}

class ByteArrayData(
    private val data: ByteArray,
) : RandomAccessData {

    override fun read(offset: Long): RawSource {
        return data.asSource(offset.toInt())
    }

    override fun close() = Unit
}

class FileData(
    private val path: Path,
) : RandomAccessData {

    private val fileHandleDelegate: Lazy<FileHandle> = lazy {
        FileSystem.SYSTEM.openReadOnly(path.toString().toPath())
    }
    private val fileHandle: FileHandle by fileHandleDelegate

    override fun read(offset: Long): RawSource {
        if (offset == 0L) {
            return SystemFileSystem.source(path)
        }
        require(offset >= 0) { "offset ($offset) < 0" }
        val okioSource = fileHandle.source(offset).buffer()
        return object : RawSource {
            override fun readAtMostTo(sink: Buffer, byteCount: Long): Long {
                if (okioSource.exhausted()) return -1
                var bytesRead = 0L
                while (bytesRead < byteCount && !okioSource.exhausted()) {
                    val byte = okioSource.readByte()
                    sink.writeByte(byte)
                    bytesRead++
                }
                return bytesRead
            }

            override fun close() {
                okioSource.close()
            }
        }
    }

    override fun close() {
        if (fileHandleDelegate.isInitialized()) {
            fileHandle.close()
        }
    }
}
