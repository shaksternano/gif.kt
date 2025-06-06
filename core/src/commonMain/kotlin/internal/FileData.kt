package com.shakster.gifkt.internal

import com.shakster.gifkt.RandomAccessData
import kotlinx.io.RawSource
import kotlinx.io.files.Path
import kotlinx.io.okio.asKotlinxIoRawSource
import okio.FileHandle
import okio.FileSystem
import okio.Path.Companion.toPath
import okio.Path as OkioPath

internal class FileData(
    private val path: OkioPath,
    private val fileSystem: FileSystem,
) : RandomAccessData {

    constructor(
        path: Path,
        fileSystem: FileSystem,
    ) : this(
        path.toString().toPath(),
        fileSystem,
    )

    private val fileHandle: FileHandle = fileSystem.openReadOnly(path)

    override fun source(offset: Long): RawSource {
        require(offset >= 0) {
            "offset ($offset) < 0"
        }
        return fileHandle.source(offset).asKotlinxIoRawSource()
    }

    override fun close() {
        fileHandle.close()
    }

    override fun toString(): String {
        return "FileData(path=$path, fileSystem=$fileSystem)"
    }
}
