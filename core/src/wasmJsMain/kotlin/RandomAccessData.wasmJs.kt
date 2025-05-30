package com.shakster.gifkt

import okio.FileSystem

internal actual val SYSTEM_FILE_SYSTEM: FileSystem
    get() = throw UnsupportedOperationException("FileSystem is not supported on WASM JS target")
