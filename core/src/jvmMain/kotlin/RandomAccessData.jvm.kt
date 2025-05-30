@file:JvmName("RandomAccessDataJvm")

package com.shakster.gifkt

import com.shakster.gifkt.internal.FileData
import okio.FileSystem
import java.io.File

internal actual val SYSTEM_FILE_SYSTEM: FileSystem = FileSystem.SYSTEM

fun java.nio.file.Path.asRandomAccess(): RandomAccessData = FileData(
    toString(),
    SYSTEM_FILE_SYSTEM,
)

fun File.asRandomAccess(): RandomAccessData = FileData(
    toString(),
    SYSTEM_FILE_SYSTEM,
)
