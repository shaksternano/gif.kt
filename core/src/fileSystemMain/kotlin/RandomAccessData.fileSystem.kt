package com.shakster.gifkt

import kotlinx.io.files.Path

expect fun RandomAccessData.Companion.of(path: Path): RandomAccessData

fun Path.asRandomAccess(): RandomAccessData {
    return RandomAccessData.of(this)
}
