package io.github.shaksternano.gifcodec

import kotlinx.io.*

internal fun Source.readLittleEndianShort(): Int = readShortLe().toInt()

/**
 * Used to identify which part of the GIF file caused an exception.
 */
private inline fun <T> readGifSection(name: String, block: () -> T): T {
    try {
        return block()
    } catch (t: Throwable) {
        throw t as? InvalidGifException ?: InvalidGifException("Failed to read GIF $name", t)
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

internal data class LogicalScreenDescriptor(
    val width: Int,
    val height: Int,
    val globalColorTableBytes: Int,
    val backgroundColorIndex: Int,
)

internal fun Source.readGifGlobalColorTable(size: Int): ByteArray = readGifSection("global color table") {
    readGifColorTable(size)
}

internal fun Source.readGifContentPart(): GifBlock = readGifSection("content") {
    if (exhausted()) return GifTerminator
    val blockIntroducer = readUByte().toInt()
    when (blockIntroducer) {
        0x21 -> readGifExtension()
        0x2C -> readGifImage()
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

        val transparentColorIndex = subBlocks[3].toUByte().toInt()

        GraphicsControlExtension(
            disposalMethod,
            transparentColorFlag,
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
    val comment = readGifSubBlocks().decodeToString()
    CommentExtension(comment)
}

private fun Source.readGifUnknownExtension(label: Int): UnknownExtension =
    readGifSection("unknown extension: ${label.toHexByteString()}") {
        skipGifSubBlocks()
        UnknownExtension
    }

private fun Source.readGifImage(): GifImage = readGifSection("image") {
    val imageDescriptor = readGifImageDescriptor()
    val localColorTable = if (imageDescriptor.localColorTableBytes > 0) {
        readGifLocalColorTable(imageDescriptor.localColorTableBytes)
    } else null
    val imageData = readGifImageData()
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
    val localColorTableBytes = if (localColorTableFlag) {
        val localColorTableSize = packed and 0b00000111
        calculateColorTableBytes(localColorTableSize)
    } else 0

    ImageDescriptor(
        left,
        top,
        width,
        height,
        localColorTableBytes,
    )
}

private fun Source.readGifLocalColorTable(size: Int): ByteArray = readGifSection("local color table") {
    readGifColorTable(size)
}

private fun Source.readGifImageData(): ImageData = readGifSection("image data") {
    val lzwMinimumCodeSize = readUByte().toInt()
    val data = readGifSubBlocks()
    ImageData(lzwMinimumCodeSize, data)
}

private fun calculateColorTableBytes(colorTableSize: Int): Int {
    /*
     * Color table bytes = 2^(n + 1) * 3:
     *     2^(n + 1) colors, where n is the color table size
     *     3 bytes per color
     */
    return 2.pow(colorTableSize + 1) * 3
}

internal fun Source.readGifColorTable(size: Int): ByteArray {
    return readByteArray(size)
}

private fun Source.readGifSubBlocks(): ByteArray {
    val data = mutableListOf<Byte>()
    var blockSize = readUByte().toInt()
    while (blockSize != 0) {
        data.addAll(readByteArray(blockSize).asList())
        blockSize = readUByte().toInt()
    }
    return data.toByteArray()
}

private fun Source.skipGifSubBlocks() {
    var blockSize = readUByte().toLong()
    while (blockSize != 0L) {
        skip(blockSize)
        blockSize = readUByte().toLong()
    }
}
