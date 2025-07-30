package com.shakster.gifkt

import kotlinx.io.Buffer
import kotlinx.io.Source
import kotlinx.io.UnsafeIoApi
import kotlinx.io.unsafe.UnsafeBufferOperations

/**
 * Creates a [Source] reading from this byte array.
 *
 * @param offset The offset in the byte array from which to start reading.
 *
 * @return A [Source] that reads from the byte array starting at the specified [offset].
 */
fun ByteArray.source(offset: Int = 0): Source {
    val buffer = Buffer()
    @OptIn(UnsafeIoApi::class)
    UnsafeBufferOperations.moveToTail(buffer, this, offset)
    return buffer
}
