package com.shakster.gifkt

import kotlinx.io.IOException
import kotlinx.io.RawSource

expect interface RandomAccessData : AutoCloseable {

    @Throws(IOException::class)
    fun source(offset: Long = 0): RawSource

    companion object {
        fun of(byteArray: ByteArray): RandomAccessData
    }

    override fun close()
}

fun ByteArray.asRandomAccess(): RandomAccessData {
    return RandomAccessData.of(this)
}
