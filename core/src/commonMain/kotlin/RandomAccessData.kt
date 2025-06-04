package com.shakster.gifkt

import kotlinx.io.RawSource

expect interface RandomAccessData : AutoCloseable {

    fun source(offset: Long = 0): RawSource

    companion object {
        fun of(bytes: ByteArray): RandomAccessData
    }

    override fun close()
}

fun ByteArray.asRandomAccess(): RandomAccessData {
    return RandomAccessData.of(this)
}
