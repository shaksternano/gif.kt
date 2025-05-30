package com.shakster.gifkt

import com.shakster.gifkt.internal.ByteArrayData
import com.shakster.gifkt.internal.FileData
import kotlinx.io.RawSource
import kotlinx.io.files.Path
import okio.FileSystem

interface RandomAccessData : AutoCloseable {
    fun source(offset: Long = 0): RawSource
}

internal expect val SYSTEM_FILE_SYSTEM: FileSystem

fun Path.asRandomAccess(): RandomAccessData = FileData(this, SYSTEM_FILE_SYSTEM)

fun ByteArray.asRandomAccess(): RandomAccessData = ByteArrayData(this)
