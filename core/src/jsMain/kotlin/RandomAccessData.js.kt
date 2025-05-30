package com.shakster.gifkt

import okio.FileSystem
import okio.NodeJsFileSystem

private val IS_NODE_JS: Boolean = js("typeof process !== 'undefined' && process.versions && !!process.versions.node")

internal actual val SYSTEM_FILE_SYSTEM: FileSystem
    get() = if (IS_NODE_JS) {
        NodeJsFileSystem
    } else {
        throw UnsupportedOperationException("Cannot access file system in a non-Node.js environment")
    }
