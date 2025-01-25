package io.github.shaksternano.gifcodec

import kotlinx.io.*

internal const val BYTES_PER_COLOR: Int = 3

internal fun Source.readLittleEndianShort(): Int = readShortLe().toInt()

/**
 * Used to identify which part of the GIF file caused an exception.
 */
internal inline fun <T> readGifSection(name: String = "", block: () -> T): T {
    try {
        return block()
    } catch (e: InvalidGifException) {
        throw e
    } catch (t: Throwable) {
        var message = "Failed to read GIF"
        if (name.isNotBlank()) {
            message += " $name"
        }
        throw InvalidGifException(message, t)
    }
}

internal fun Source.readGifIntroduction(): GifIntroduction {
    readGifHeader()
    val logicalScreenDescriptor = readGifLogicalScreenDescriptor()
    val globalColorTable = if (logicalScreenDescriptor.globalColorTableColors > 0) {
        readGifGlobalColorTable(BYTES_PER_COLOR * logicalScreenDescriptor.globalColorTableColors)
    } else null
    return GifIntroduction(logicalScreenDescriptor, globalColorTable)
}

internal data class GifIntroduction(
    val logicalScreenDescriptor: LogicalScreenDescriptor,
    val globalColorTable: ByteArray?,
) {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as GifIntroduction

        if (logicalScreenDescriptor != other.logicalScreenDescriptor) return false
        if (!globalColorTable.contentEquals(other.globalColorTable)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = logicalScreenDescriptor.hashCode()
        result = 31 * result + (globalColorTable?.contentHashCode() ?: 0)
        return result
    }
}

internal fun Source.readGifHeader() = readGifSection("header") {
    val header = readByteArray(6)
    val headerString = header.decodeToString()
    if (!headerString.startsWith("GIF")) {
        throw InvalidGifException("File doesn't start with GIF header")
    }
}

internal fun Source.readGifLogicalScreenDescriptor(): LogicalScreenDescriptor =
    readGifSection("logical screen descriptor") {
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
        val globalColorTableColors: Int
        val backgroundColorIndex: Int
        if (globalColorTableFlag) {
            // Bits 6-8
            val globalColorTableSize = packed and 0b00000111
            globalColorTableColors = calculateColorTableColors(globalColorTableSize)
            backgroundColorIndex = readByte().toInt()
        } else {
            globalColorTableColors = 0
            backgroundColorIndex = 0
            // Background color index
            skip(1)
        }
        // Pixel aspect ratio
        skip(1)
        LogicalScreenDescriptor(
            width,
            height,
            globalColorTableColors,
            backgroundColorIndex,
        )
    }

internal data class LogicalScreenDescriptor(
    val width: Int,
    val height: Int,
    val globalColorTableColors: Int,
    val backgroundColorIndex: Int,
)

internal fun Source.readGifGlobalColorTable(size: Int): ByteArray = readGifSection("global color table") {
    readGifColorTable(size)
}

internal fun Source.readGifBlock(decodeImage: Boolean, globalColorTableColors: Int): GifBlock =
    readGifSection("content") {
    if (exhausted()) return GifTerminator
    val blockIntroducer = readUByte().toInt()
    when (blockIntroducer) {
        0x21 -> readGifExtension()
        0x2C -> readGifImage(decodeImage, globalColorTableColors)
        0x3B -> GifTerminator
        else -> throw InvalidGifException("Unknown block introducer: ${blockIntroducer.toHexByteString()}")
    }
}

private fun Source.readGifExtension(): GifExtension = readGifSection("extension") {
    val extensionLabel = readUByte().toInt()
    when (extensionLabel) {
        0xF9 -> readGifGraphicsControlExtension()
        0xFF -> readGifApplicationExtension()
        0xFE -> readGifCommentExtension()
        else -> readGifUnknownExtension(extensionLabel)
    }
}

private fun Source.readGifGraphicsControlExtension(): GraphicsControlExtension =
    readGifSection("graphics control extension") {
        val subBlocks = readGifSubBlocks()

        /*
         * Bits:
         * 1-3 : Reserved for future use
         * 4-6 : Disposal method
         * 7   : User input flag
         * 8   : Transparent color flag
         */
        val packed = subBlocks[0].toUByte().toInt()

        // Bits 4-6
        val disposalMethodId = packed and 0b00011100 shr 2
        val disposalMethod = DisposalMethod.fromId(disposalMethodId)

        // Bit 7
        val transparentColorFlag = packed and 0b00000001 != 0

        // Delay time in little-endian format
        val delayTimeLow = subBlocks[1].toUByte().toInt()
        val delayTimeHigh = subBlocks[2].toUByte().toInt()
        val delayTime = delayTimeLow or (delayTimeHigh shl 8)

        val transparentColorIndex = if (transparentColorFlag) {
            subBlocks[3].toUByte().toInt()
        } else -1

        GraphicsControlExtension(
            disposalMethod,
            delayTime,
            transparentColorIndex,
        )
    }

private fun Source.readGifApplicationExtension(): ApplicationExtension = readGifSection("application extension") {
    val identifierSize = readUByte().toLong()
    val identifier = readString(identifierSize)
    when (identifier) {
        "NETSCAPE2.0" -> readGifNetscapeApplicationExtension()
        else -> readGifUnknownApplicationExtension(identifier)
    }
}

private fun Source.readGifNetscapeApplicationExtension(): NetscapeApplicationExtension =
    readGifSection("Netscape application extension") {
        val data = readGifSubBlocks()
        val loopCountLow = data[1].toUByte().toInt()
        val loopCountHigh = data[2].toUByte().toInt()
        val loopCount = loopCountLow or (loopCountHigh shl 8)
        NetscapeApplicationExtension(loopCount)
    }

private fun Source.readGifUnknownApplicationExtension(identifier: String): UnknownApplicationExtension =
    readGifSection("unknown application extension: $identifier") {
        skipGifSubBlocks()
        UnknownApplicationExtension
    }

private fun Source.readGifCommentExtension(): CommentExtension = readGifSection("comment extension") {
    val comment = readGifSubBlocks().toByteArray().decodeToString()
    CommentExtension(comment)
}

private fun Source.readGifUnknownExtension(label: Int): UnknownExtension =
    readGifSection("unknown extension: ${label.toHexByteString()}") {
        skipGifSubBlocks()
        UnknownExtension
    }

private fun Source.readGifImage(decodeImage: Boolean, globalColorTableColors: Int): GifImage = readGifSection("image") {
    val imageDescriptor = readGifImageDescriptor()
    val localColorTable = if (imageDescriptor.localColorTableColors > 0) {
        readGifLocalColorTable(BYTES_PER_COLOR * imageDescriptor.localColorTableColors)
    } else null
    val imageData = readGifImageData(decodeImage, globalColorTableColors)
    GifImage(imageDescriptor, localColorTable, imageData)
}

private fun Source.readGifImageDescriptor(): ImageDescriptor = readGifSection("image descriptor") {
    val left = readLittleEndianShort()
    val top = readLittleEndianShort()
    val width = readLittleEndianShort()
    val height = readLittleEndianShort()

    /*
     * Bits:
     * 1   : Local color table flag
     * 2   : Interlace flag
     * 3   : Sort flag
     * 4-5 : Reserved for future use
     * 6-8 : Size of local color table
     */
    val packed = readUByte().toInt()
    // Bit 1
    val localColorTableFlag = packed and 0b10000000 != 0
    // Bits 6-8
    val localColorTableColors = if (localColorTableFlag) {
        val localColorTableSize = packed and 0b00000111
        calculateColorTableColors(localColorTableSize)
    } else 0

    ImageDescriptor(
        left,
        top,
        width,
        height,
        localColorTableColors,
    )
}

private fun Source.readGifLocalColorTable(size: Int): ByteArray = readGifSection("local color table") {
    readGifColorTable(size)
}

private fun Source.readGifImageData(decodeImage: Boolean, maxColors: Int): ImageData =
    readGifSection("LZW image data") {
        if (decodeImage) {
            val indices = readLzwIndexStream(maxColors)
            DecodedImageData(indices)
        } else {
            // LZW minimum code size
            skip(1)
            skipGifSubBlocks()
            IgnoredImageData
        }
}

private fun calculateColorTableColors(colorTableSize: Int): Int {
    /*
     * The number of colors in a color table is 2^(n + 1)s,
     * where n is the color table size.
     */
    return 2.pow(colorTableSize + 1)
}

internal fun Source.readGifColorTable(size: Int): ByteArray {
    return readByteArray(size)
}

private fun Source.readGifSubBlocks(): ByteList {
    val data = ByteList()
    var blockSize = readUByte().toInt()
    while (blockSize != 0) {
        repeat(blockSize) {
            data += readByte()
        }
        blockSize = readUByte().toInt()
    }
    return data
}

private fun Source.skipGifSubBlocks() {
    var blockSize = readUByte().toLong()
    while (blockSize != 0L) {
        skip(blockSize)
        blockSize = readUByte().toLong()
    }
}
