package com.shakster.gifkt.internal

import com.shakster.gifkt.RandomAccessData
import kotlinx.io.EOFException
import kotlinx.io.IOException
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

    private val fileHandle: FileHandle = withOkio2KxIOExceptionMapping {
        fileSystem.openReadOnly(path)
    }

    override fun source(offset: Long): RawSource = withOkio2KxIOExceptionMapping {
        require(offset >= 0) {
            "offset ($offset) < 0"
        }
        return fileHandle.source(offset).asKotlinxIoRawSource()
    }

    override fun close() = withOkio2KxIOExceptionMapping {
        fileHandle.close()
    }

    override fun toString(): String {
        return "FileData(path=$path, fileSystem=$fileSystem)"
    }

    private inline fun <T> withOkio2KxIOExceptionMapping(block: () -> T): T {
        try {
            return block()
        } catch (e: IOException) { // on JVM, kotlinx.io.IOException and okio.IOException are the same
            throw e
        } catch (e: EOFException) { // see above
            throw e
        } catch (e: okio.EOFException) {
            throw EOFException(e.message)
        } catch (e: okio.IOException) {
            throw IOException(e.message, e)
        }
    }
}
