package com.shakster.gifkt.internal

import com.shakster.gifkt.*
import kotlinx.io.EOFException
import kotlinx.io.IOException
import kotlinx.io.Source
import kotlin.time.Duration

internal const val BYTES_PER_COLOR: Int = 3

internal inline fun Source.readGif(
    decodeImages: Boolean,
    onImageDecode: (image: ImageFrame) -> Unit = {},
): GifInfo {
    val monitoredSource = monitored()

    val introduction = monitoredSource.readGifIntroduction()
    val canvasWidth = introduction.logicalScreenDescriptor.width
    val canvasHeight = introduction.logicalScreenDescriptor.height
    val backgroundColorIndex = introduction.logicalScreenDescriptor.backgroundColorIndex
    val globalColorTableColors = introduction.logicalScreenDescriptor.globalColorTableColors
    val globalColorTable = introduction.globalColorTable

    return monitoredSource.readGifContent(
        decodeImages,
        canvasWidth,
        canvasHeight,
        globalColorTable,
        globalColorTableColors,
        backgroundColorIndex,
        onImageDecode,
    )
}

private inline fun MonitoredSource.readGifContent(
    decodeImages: Boolean,
    canvasWidth: Int,
    canvasHeight: Int,
    globalColorTable: ByteArray?,
    globalColorTableColors: Int,
    backgroundColorIndex: Int,
    onImageDecode: (image: ImageFrame) -> Unit,
): GifInfo {
    var loopCount = -1
    var comment = ""

    val frames = mutableListOf<RawImage>()

    var currentDisposalMethod = DisposalMethod.UNSPECIFIED
    var currentDelayTime = 0
    var currentTransparentColorIndex = -1

    var frameIndex = 0
    var previousImage: IntArray? = null
    var timestamp = Duration.ZERO

    var nextIsKeyFrame = true

    readGifBlocks(decodeImages) { byteOffset, block ->
        when (block) {
            is GraphicsControlExtension -> {
                currentDisposalMethod = block.disposalMethod
                currentDelayTime = block.delayTime
                currentTransparentColorIndex = block.transparentColorIndex
            }

            is NetscapeApplicationExtension -> {
                if (loopCount < 0) {
                    loopCount = block.loopCount
                }
            }

            is CommentExtension -> {
                if (comment.isBlank()) {
                    comment = block.comment
                }
            }

            is GifImage -> {
                val duration = currentDelayTime.centiseconds
                val currentColorTable = block.localColorTable ?: globalColorTable
                ?: throw InvalidGifException(
                    "Frame $frameIndex has no color table, byte offset $byteOffset"
                )

                val imageDescriptor = block.descriptor
                if (decodeImages) {
                    val imageFrame = readImage(
                        canvasWidth,
                        canvasHeight,
                        imageDescriptor,
                        block.colorIndices,
                        globalColorTable,
                        globalColorTableColors,
                        currentColorTable,
                        backgroundColorIndex,
                        currentTransparentColorIndex,
                        previousImage,
                        duration,
                        timestamp,
                        frameIndex,
                    )

                    onImageDecode(imageFrame)

                    val disposedImage = disposeImage(
                        imageFrame.argb,
                        previousImage,
                        currentDisposalMethod,
                        canvasWidth,
                        canvasHeight,
                        imageDescriptor.left,
                        imageDescriptor.top,
                        imageDescriptor.width,
                        imageDescriptor.height,
                        usesGlobalColorTable = block.localColorTable == null,
                        globalColorTable,
                        globalColorTableColors,
                        backgroundColorIndex,
                    )
                    previousImage = disposedImage
                }

                val frame = RawImage(
                    null,
                    imageDescriptor.left,
                    imageDescriptor.top,
                    imageDescriptor.width,
                    imageDescriptor.height,
                    usesLocalColorTable = block.localColorTable != null,
                    currentTransparentColorIndex,
                    currentDisposalMethod,
                    duration,
                    timestamp,
                    frameIndex,
                    byteOffset = byteOffset,
                    isKeyFrame = nextIsKeyFrame,
                )
                frames.add(frame)

                nextIsKeyFrame = currentDisposalMethod == DisposalMethod.RESTORE_TO_BACKGROUND_COLOR
                    && imageDescriptor.left == 0
                    && imageDescriptor.top == 0
                    && imageDescriptor.width == canvasWidth
                    && imageDescriptor.height == canvasHeight

                // Reset values for next frame
                currentDisposalMethod = DisposalMethod.UNSPECIFIED
                currentDelayTime = 0
                currentTransparentColorIndex = -1

                timestamp += duration
                frameIndex++
            }

            else -> Unit
        }
    }

    return GifInfo(
        canvasWidth,
        canvasHeight,
        frameIndex,
        timestamp,
        loopCount,
        globalColorTable,
        globalColorTableColors,
        backgroundColorIndex,
        frames,
        comment,
    )
}

private inline fun MonitoredSource.readGifBlocks(
    decodeImages: Boolean,
    action: (byteOffset: Long, block: GifBlock) -> Unit,
) {
    var byteOffset = this.bytesRead
    var block = readGifBlock(decodeImages)
    while (block != GifTerminator) {
        action(byteOffset, block)
        byteOffset = this.bytesRead
        block = readGifBlock(decodeImages)
    }
}

private fun readImage(
    canvasWidth: Int,
    canvasHeight: Int,
    imageDescriptor: ImageDescriptor,
    colorIndices: ByteList,
    globalColorTable: ByteArray?,
    globalColorTableColors: Int,
    currentColorTable: ByteArray,
    backgroundColorIndex: Int,
    currentTransparentColorIndex: Int,
    previousImage: IntArray?,
    duration: Duration,
    timestamp: Duration,
    frameIndex: Int,
): ImageFrame {
    val imageLeft = imageDescriptor.left
    val imageTop = imageDescriptor.top
    val imageWidth = imageDescriptor.width
    val imageHeight = imageDescriptor.height

    val image = getImageArgb(
        canvasWidth,
        canvasHeight,
        imageLeft,
        imageTop,
        imageWidth,
        imageHeight,
        colorIndices,
        globalColorTable,
        globalColorTableColors,
        currentColorTable,
        backgroundColorIndex,
        currentTransparentColorIndex,
        previousImage,
    )

    return ImageFrame(
        image,
        canvasWidth,
        canvasHeight,
        duration,
        timestamp,
        frameIndex,
    )
}

internal fun getImageArgb(
    canvasWidth: Int,
    canvasHeight: Int,
    imageLeft: Int,
    imageTop: Int,
    imageWidth: Int,
    imageHeight: Int,
    colorIndices: ByteList,
    globalColorTable: ByteArray?,
    globalColorTableColors: Int,
    currentColorTable: ByteArray,
    backgroundColorIndex: Int,
    currentTransparentColorIndex: Int,
    previousImage: IntArray?,
): IntArray {
    val canvasSize = canvasWidth * canvasHeight
    return IntArray(canvasSize) { i ->
        val absoluteX = i % canvasWidth
        val absoluteY = i / canvasWidth

        val relativeX = absoluteX - imageLeft
        val relativeY = absoluteY - imageTop

        val colorIndex = run {
            if (relativeX in 0..<imageWidth && relativeY in 0..<imageHeight) {
                val index = relativeY * imageWidth + relativeX
                if (index in colorIndices.indices) {
                    return@run colorIndices[index].toInt() and 0xFF
                }
            }
            /*
             * Missing indices use the background color if available,
             * otherwise they are transparent.
             */
            -1
        }

        val useBackgroundColor = colorIndex < 0
        if (useBackgroundColor || colorIndex == currentTransparentColorIndex) {
            if (previousImage != null) {
                previousImage[i]
            } else if (
                useBackgroundColor
                && currentColorTable === globalColorTable
                && backgroundColorIndex != currentTransparentColorIndex
                && backgroundColorIndex in 0..<globalColorTableColors
            ) {
                getColor(globalColorTable, backgroundColorIndex)
            } else {
                // Transparent
                0
            }
        } else {
            getColor(currentColorTable, colorIndex)
        }
    }
}

internal fun disposeImage(
    image: IntArray,
    previousImage: IntArray?,
    disposalMethod: DisposalMethod,
    canvasWidth: Int,
    canvasHeight: Int,
    imageLeft: Int,
    imageTop: Int,
    imageWidth: Int,
    imageHeight: Int,
    usesGlobalColorTable: Boolean,
    globalColorTable: ByteArray?,
    globalColorTableColors: Int,
    backgroundColorIndex: Int,
): IntArray? = when (disposalMethod) {
    DisposalMethod.UNSPECIFIED -> image
    DisposalMethod.DO_NOT_DISPOSE -> image
    DisposalMethod.RESTORE_TO_BACKGROUND_COLOR -> {
        val disposeAll = previousImage == null || (
            imageLeft <= 0
                && imageTop <= 0
                && imageLeft + imageWidth >= canvasWidth
                && imageTop + imageHeight >= canvasHeight
            )
        if (disposeAll) {
            null
        } else {
            val backgroundColor = if (
                usesGlobalColorTable
                && globalColorTable != null
                && backgroundColorIndex in 0..<globalColorTableColors
            ) {
                getColor(globalColorTable, backgroundColorIndex)
            } else {
                // Transparent
                0
            }

            val newPreviousImage = previousImage.copyOf()
            for (y in imageTop..<imageTop + imageHeight) {
                for (x in imageLeft..<imageLeft + imageWidth) {
                    if (x >= canvasWidth || y >= canvasHeight) {
                        continue
                    }
                    val i = y * canvasWidth + x
                    newPreviousImage[i] = backgroundColor
                }
            }
            newPreviousImage
        }
    }

    DisposalMethod.RESTORE_TO_PREVIOUS -> previousImage
}

/**
 * Used to identify which part of the GIF file caused an exception.
 */
internal inline fun <T> MonitoredSource.readGifSection(name: String, block: () -> T): T {
    val sectionStart = bytesRead
    try {
        return block()
    } catch (e: InvalidGifException) {
        throw e
    } catch (e: IOException) {
        throw e
    } catch (t: Throwable) {
        throw InvalidGifException(
            "Failed to read GIF $name" +
                ", starting at byte offset $sectionStart" +
                ", ending at byte offset $bytesRead",
            t,
        )
    }
}

internal fun MonitoredSource.readGifIntroduction(): GifIntroduction {
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

internal fun MonitoredSource.readGifHeader() = readGifSection("header") {
    val header = readByteArray(6)
    val headerString = header.decodeToString()
    if (!headerString.startsWith("GIF")) {
        throw InvalidGifException("File doesn't start with GIF header")
    }
}

internal fun MonitoredSource.readGifLogicalScreenDescriptor(): LogicalScreenDescriptor =
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

internal fun MonitoredSource.readGifGlobalColorTable(size: Int): ByteArray = readGifSection("global color table") {
    readGifColorTable(size)
}

internal fun MonitoredSource.readGifBlock(
    decodeImage: Boolean,
): GifBlock = readGifSection("content") {
    try {
        if (exhausted()) return GifTerminator
        val blockStart = bytesRead
        when (val blockIntroducer = readByte().toInt() and 0xFF) {
            0x21 -> readGifExtension()
            0x2C -> readGifImage(decodeImage)
            0x3B -> GifTerminator
            else -> throw InvalidGifException(
                "Unknown block introducer at byte offset $blockStart: ${blockIntroducer.toHexByteString()}"
            )
        }
    } catch (_: EOFException) {
        GifTerminator
    }
}

private fun MonitoredSource.readGifExtension(): GifExtension = readGifSection("extension") {
    when (val extensionLabel = readByte().toInt() and 0xFF) {
        0xF9 -> readGifGraphicsControlExtension()
        0xFF -> readGifApplicationExtension()
        0xFE -> readGifCommentExtension()
        else -> readGifUnknownExtension(extensionLabel)
    }
}

private fun MonitoredSource.readGifGraphicsControlExtension(): GraphicsControlExtension =
    readGifSection("graphics control extension") {
        val subBlocks = readGifSubBlocks()

        /*
         * Bits:
         * 1-3 : Reserved for future use
         * 4-6 : Disposal method
         * 7   : User input flag
         * 8   : Transparent color flag
         */
        val packed = subBlocks[0].toInt() and 0xFF

        // Bits 4-6
        val disposalMethodId = packed and 0b00011100 shr 2
        val disposalMethod = DisposalMethod.fromId(disposalMethodId)

        // Bit 7
        val transparentColorFlag = packed and 0b00000001 != 0

        // Delay time in little-endian format
        val delayTimeLow = subBlocks[1].toInt() and 0xFF
        val delayTimeHigh = subBlocks[2].toInt() and 0xFF
        val delayTime = delayTimeLow or (delayTimeHigh shl 8)

        val transparentColorIndex = if (transparentColorFlag) {
            subBlocks[3].toInt() and 0xFF
        } else -1

        GraphicsControlExtension(
            disposalMethod,
            delayTime,
            transparentColorIndex,
        )
    }

private fun MonitoredSource.readGifApplicationExtension(): ApplicationExtension =
    readGifSection("application extension") {
        val identifierSize = readByte().toLong() and 0xFF
        when (val identifier = readString(identifierSize)) {
            "NETSCAPE2.0" -> readGifNetscapeApplicationExtension()
            else -> readGifUnknownApplicationExtension(identifier)
        }
    }

private fun MonitoredSource.readGifNetscapeApplicationExtension(): NetscapeApplicationExtension =
    readGifSection("Netscape application extension") {
        val data = readGifSubBlocks()
        val loopCountLow = data[1].toInt() and 0xFF
        val loopCountHigh = data[2].toInt() and 0xFF
        val loopCount = loopCountLow or (loopCountHigh shl 8)
        NetscapeApplicationExtension(loopCount)
    }

private fun MonitoredSource.readGifUnknownApplicationExtension(identifier: String): UnknownApplicationExtension =
    readGifSection("unknown application extension: $identifier") {
        skipGifSubBlocks()
        UnknownApplicationExtension
    }

private fun MonitoredSource.readGifCommentExtension(): CommentExtension =
    readGifSection("comment extension") {
        val comment = readGifSubBlocks().decodeToString()
        CommentExtension(comment)
    }

private fun MonitoredSource.readGifUnknownExtension(label: Int): UnknownExtension =
    readGifSection("unknown extension: ${label.toHexByteString()}") {
        skipGifSubBlocks()
        UnknownExtension
    }

internal fun MonitoredSource.readGifImage(
    decodeImage: Boolean,
): GifImage = readGifSection("image") {
    val imageDescriptor = readGifImageDescriptor()
    val localColorTable = if (imageDescriptor.localColorTableColors > 0) {
        readGifLocalColorTable(BYTES_PER_COLOR * imageDescriptor.localColorTableColors)
    } else null
    val indices = if (decodeImage) {
        readGifImageData()
    } else {
        // LZW minimum code size
        skip(1)
        // LZW image data
        skipGifSubBlocks()
        ByteList()
    }
    GifImage(imageDescriptor, localColorTable, indices)
}

private fun MonitoredSource.readGifImageDescriptor(): ImageDescriptor = readGifSection("image descriptor") {
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
    val packed = readByte().toInt() and 0xFF
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

private fun MonitoredSource.readGifLocalColorTable(size: Int): ByteArray = readGifSection("local color table") {
    readGifColorTable(size)
}

private fun MonitoredSource.readGifImageData(): ByteList = readGifSection("LZW image data") {
    readLzwIndexStream()
}

private fun calculateColorTableColors(colorTableSize: Int): Int {
    /*
     * The number of colors in a color table is 2^(n + 1)s,
     * where n is the color table size.
     */
    return 2.pow(colorTableSize + 1)
}

internal fun MonitoredSource.readGifColorTable(size: Int): ByteArray {
    return readByteArray(size)
}

private fun MonitoredSource.readGifSubBlocks(): ByteList {
    val data = ByteList()
    var blockSize = readByte().toInt() and 0xFF
    while (blockSize != 0) {
        data += readByteArray(blockSize)
        blockSize = readByte().toInt() and 0xFF
    }
    return data
}

private fun MonitoredSource.skipGifSubBlocks() {
    var blockSize = readByte().toLong() and 0xFF
    while (blockSize != 0L) {
        skip(blockSize)
        blockSize = readByte().toLong() and 0xFF
    }
}

private fun getColor(colorTable: ByteArray, index: Int): Int {
    val colorIndex = index * BYTES_PER_COLOR
    val red = colorTable[colorIndex].toInt()
    val green = colorTable[colorIndex + 1].toInt()
    val blue = colorTable[colorIndex + 2].toInt()
    return RGB(red, green, blue).value
}
