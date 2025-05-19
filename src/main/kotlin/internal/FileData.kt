package io.github.shaksternano.gifcodec.internal

import io.github.shaksternano.gifcodec.RandomAccessData
import kotlinx.io.RawSource
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import kotlinx.io.okio.asKotlinxIoRawSource
import okio.FileHandle
import okio.FileSystem
import okio.Path.Companion.toPath

internal class FileData(
    private val path: Path,
) : RandomAccessData {

    private lateinit var fileHandle: FileHandle

    override fun read(offset: Long): RawSource {
        if (offset == 0L) {
            return SystemFileSystem.source(path)
        }
        require(offset >= 0) { "offset ($offset) < 0" }
        if (!::fileHandle.isInitialized) {
            fileHandle = FileSystem.SYSTEM.openReadOnly(path.toString().toPath())
        }
        return fileHandle.source(offset).asKotlinxIoRawSource()
    }

    override fun close() {
        if (::fileHandle.isInitialized) {
            fileHandle.close()
        }
    }
}
