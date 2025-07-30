package com.shakster.gifkt

import com.shakster.gifkt.internal.ByteArrayData
import kotlinx.io.IOException
import kotlinx.io.RawSource

/**
 * A source of data which supports random access reading.
 */
actual interface RandomAccessData : AutoCloseable {

    /**
     * Creates a [RawSource] that reads from this data.
     *
     * @param offset The offset in the data from which to start reading.
     *
     * @return A [RawSource] that reads from the data starting at the specified [offset].
     *
     * @throws IOException If an I/O error occurs.
     */
    actual fun source(offset: Long): RawSource

    /**
     * Closes this data source, releasing any resources held by it.
     *
     * @throws IOException If an I/O error occurs.
     */
    actual override fun close()

    actual companion object {
        /**
         * Creates a [RandomAccessData] instance that reads from a byte array.
         *
         * @param bytes The byte array to read from.
         *
         * @return A [RandomAccessData] instance that reads from a byte array.
         */
        actual fun of(bytes: ByteArray): RandomAccessData {
            return ByteArrayData(bytes)
        }
    }
}
