package com.shakster.gifkt

import okio.FileSystem
import okio.WasiFileSystem

internal actual val SYSTEM_FILE_SYSTEM: FileSystem = WasiFileSystem
