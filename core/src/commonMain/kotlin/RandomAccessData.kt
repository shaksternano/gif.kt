package com.shakster.gifkt

import kotlinx.io.RawSource

interface RandomAccessData : AutoCloseable {
    fun read(offset: Long = 0): RawSource
}
