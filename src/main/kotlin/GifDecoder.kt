package io.github.shaksternano.gifcodec

import kotlinx.io.Source
import kotlinx.io.readByteArray
import kotlin.properties.Delegates

class GifDecoder(
    private val sourceSupplier: () -> Source,
    private val maxCachedFrames: Int = 10,
) : AutoCloseable {

    private var source: Source = sourceSupplier()

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

    private inline fun <T> readGifSection(name: String, block: () -> T): T {
        try {
            return block()
        } catch (t: Throwable) {
            throw if (t is InvalidGifException) {
                t
            } else {
                InvalidGifException("Failed to read GIF $name", t)
            }
        }
    }

    private fun Source.readGifHeader() = readGifSection("header") {
        val header = readByteArray(6)
        val headerString = header.decodeToString()
        if (!headerString.startsWith("GIF")) {
            throw InvalidGifException("File doesn't start with GIF header")
        }
    }

    private fun Source.readGifLogicalScreenDescriptor(): LogicalScreenDescriptor = readGifSection("logical screen descriptor") {
        val width = readLittleEndianShort()
        val height = readLittleEndianShort()
        /*
         * Bits:
         * 1   : global color table flag
         * 2-4 : color resolution
         * 5   : global color table sort flag
         * 6-8 : global color table size
         */
        val packed = readByte().toInt()
        // Bit 1
        val globalColorTableFlag = packed and 0b10000000 != 0
        val globalColorTableBytes: Int
        val backgroundColorIndex: Int
        if (globalColorTableFlag) {
            // Bits 6-8
            val globalColorTableSize = packed and 0b00000111
            globalColorTableBytes = calculateColorTableBytes(globalColorTableSize)
            backgroundColorIndex = readByte().toInt()
        } else {
            globalColorTableBytes = 0
            backgroundColorIndex = 0
            // Background color index
            skip(1)
        }
        // Pixel aspect ratio
        skip(1)
        LogicalScreenDescriptor(
            width,
            height,
            globalColorTableBytes,
            backgroundColorIndex,
        )
    }

    private fun Source.readGifColorTable(size: Int): ByteArray {
        return readByteArray(size)
    }

    private fun Source.readGifGlobalColorTable(size: Int): ByteArray = readGifSection("global color table") {
        readGifColorTable(size)
    }

    private fun calculateColorTableBytes(colorTableSize: Int): Int {
        /*
         * Color table bytes = 2^(n + 1) * 3:
         *     2^(n + 1) colors, where n is the color table size
         *     3 bytes per color
         */
        return 2.pow(colorTableSize + 1) * 3
    }

    private data class LogicalScreenDescriptor(
        val width: Int,
        val height: Int,
        val globalColorTableBytes: Int,
        val backgroundColorIndex: Int,
    )

    override fun close() {
        source.close()
    }
}
