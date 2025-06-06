package com.shakster.gifkt

import kotlinx.io.RawSource

expect interface RandomAccessData : AutoCloseable {

    fun source(offset: Long = 0): RawSource

    override fun close()

    companion object {
        fun of(bytes: ByteArray): RandomAccessData
    }
}

fun ByteArray.asRandomAccess(): RandomAccessData {
    return RandomAccessData.of(this)
}
