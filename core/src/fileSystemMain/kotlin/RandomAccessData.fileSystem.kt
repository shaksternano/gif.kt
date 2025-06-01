package com.shakster.gifkt

import kotlinx.io.IOException
import kotlinx.io.files.Path

@Throws(IOException::class)
expect fun RandomAccessData.Companion.of(path: Path): RandomAccessData

@Throws(IOException::class)
fun Path.asRandomAccess(): RandomAccessData {
    return RandomAccessData.of(this)
}
