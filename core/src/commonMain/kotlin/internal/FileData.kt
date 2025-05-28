package com.shakster.gifkt.internal

import com.shakster.gifkt.RandomAccessData
import kotlinx.io.RawSource
import kotlinx.io.files.Path
import kotlinx.io.okio.asKotlinxIoRawSource
import okio.FileHandle
import okio.FileSystem
import okio.Path.Companion.toPath

internal class FileData(
    path: Path,
    fileSystem: FileSystem,
) : RandomAccessData {

    private val fileHandle: FileHandle = fileSystem.openReadOnly(
        path.toString().toPath(),
    )

    override fun read(offset: Long): RawSource {
        require(offset >= 0) {
            "offset ($offset) < 0"
        }
        return fileHandle.source(offset).asKotlinxIoRawSource()
    }

    override fun close() {
        fileHandle.close()
    }
}
