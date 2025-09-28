@file:JvmName("RandomAccessDataFileSystem")

package com.shakster.gifkt

import kotlinx.io.IOException
import kotlinx.io.files.Path
import kotlin.jvm.JvmName
import kotlin.jvm.JvmSynthetic

/**
 * Creates a [RandomAccessData] instance that reads from a file.
 *
 * @param path The [Path] of the file to read from.
 *
 * @return A [RandomAccessData] instance that reads from a file.
 *
 * @throws IOException If an I/O error occurs.
 */
expect fun RandomAccessData.Companion.of(path: Path): RandomAccessData

/**
 * Creates a [RandomAccessData] instance that reads from a file.
 *
 * @receiver The [Path] of the file to read from.
 *
 * @return A [RandomAccessData] instance that reads from a file.
 *
 * @throws IOException If an I/O error occurs.
 */
@JvmSynthetic
@Throws(IOException::class)
fun Path.asRandomAccess(): RandomAccessData {
    return RandomAccessData.of(this)
}
