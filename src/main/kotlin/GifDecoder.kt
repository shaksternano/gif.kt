package io.github.shaksternano.gifcodec

import kotlinx.io.Source
import kotlinx.io.buffered
import kotlin.properties.Delegates

/*
 * Reference:
 * https://www.matthewflickinger.com/lab/whatsinagif/bits_and_bytes.asp
 */
class GifDecoder(
    private val data: RandomAccessData,
    private val maxCachedFrames: Int = 10,
) : AutoCloseable {

    val source: Source = data.read().buffered()

    private var width: Int by Delegates.notNull()
    private var height: Int by Delegates.notNull()

    private var globalColorTable: ByteArray? = null
    private var backgroundColorIndex: Int = 0

    private fun init() {
        source.readGifHeader()
        val logicalScreenDescriptor = source.readGifLogicalScreenDescriptor()
        width = logicalScreenDescriptor.width
        height = logicalScreenDescriptor.height
        if (logicalScreenDescriptor.globalColorTableBytes > 0) {
            globalColorTable = source.readGifGlobalColorTable(logicalScreenDescriptor.globalColorTableBytes)
            backgroundColorIndex = logicalScreenDescriptor.backgroundColorIndex
        }
    }

    override fun close() {
        source.close()
        data.close()
    }
}
