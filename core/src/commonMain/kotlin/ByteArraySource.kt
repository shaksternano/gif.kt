package com.shakster.gifkt

import kotlinx.io.Buffer
import kotlinx.io.Source
import kotlinx.io.UnsafeIoApi
import kotlinx.io.unsafe.UnsafeBufferOperations

fun ByteArray.source(offset: Int = 0): Source {
    val buffer = Buffer()
    @OptIn(UnsafeIoApi::class)
    UnsafeBufferOperations.moveToTail(buffer, this, offset)
    return buffer
}
