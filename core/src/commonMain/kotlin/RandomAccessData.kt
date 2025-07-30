package com.shakster.gifkt

import kotlinx.io.IOException
import kotlinx.io.RawSource
import kotlin.jvm.JvmSynthetic

/**
 * A source of data which supports random access reading.
 */
expect interface RandomAccessData : AutoCloseable {

    /**
     * Creates a [RawSource] that reads from this data.
     *
     * @param offset The offset in the data from which to start reading.
     *
     * @return A [RawSource] that reads from the data starting at the specified [offset].
     *
     * @throws IOException If an I/O error occurs.
     */
    fun source(offset: Long = 0): RawSource

    /**
     * Closes this data source, releasing any resources held by it.
     *
     * @throws IOException If an I/O error occurs.
     */
    override fun close()

    companion object {
        /**
         * Creates a [RandomAccessData] instance that reads from a byte array.
         *
         * @param bytes The byte array to read from.
         *
         * @return A [RandomAccessData] instance that reads from a byte array.
         */
        fun of(bytes: ByteArray): RandomAccessData
    }
}

/**
 * Creates a [RandomAccessData] instance that reads from a byte array.
 *
 * @receiver The byte array to read from.
 *
 * @return A [RandomAccessData] instance that reads from a byte array.
 */
@JvmSynthetic
fun ByteArray.asRandomAccess(): RandomAccessData {
    return RandomAccessData.of(this)
}
