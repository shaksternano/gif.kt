package io.github.shaksternano.gifcodec.internal

import io.github.shaksternano.gifcodec.ImageFrame
import io.github.shaksternano.gifcodec.InvalidGifException
import kotlinx.io.Source
import kotlin.time.Duration

internal const val BYTES_PER_COLOR: Int = 3
private val EMPTY_INT_ARRAY: IntArray = IntArray(0)

internal fun readGifFrames(
    keyFrameInterval: Int = 0,
    sourceSupplier: () -> Source,
): Sequence<ImageFrame> = sequence {
    sourceSupplier().use { source ->
        readGif(
            source,
            keyFrameInterval,
            decodeImages = true,
        ) {
            yield(it)
        }
    }
}

internal inline fun readGif(
    source: Source,
    keyFrameInterval: Int,
    decodeImages: Boolean,
    onImageDecode: (ImageFrame) -> Unit = {},
): GifInfo {
    val source = MonitoredSource(source)

    val introduction = source.readGifIntroduction()
    val canvasWidth = introduction.logicalScreenDescriptor.width
    val canvasHeight = introduction.logicalScreenDescriptor.height
    val backgroundColorIndex = introduction.logicalScreenDescriptor.backgroundColorIndex
    val globalColorTableColors = introduction.logicalScreenDescriptor.globalColorTableColors
    val globalColorTable = introduction.globalColorTable

    var loopCount = -1
    var comment = ""

    val frames = mutableListOf<FrameInfo>()

    var currentDisposalMethod = DisposalMethod.UNSPECIFIED
    var currentDelayTime = 0
    var currentTransparentColorIndex = -1

    var frameIndex = 0
    var previousImage: IntArray? = null
    var timestamp = Duration.ZERO

    var bytesRead = source.bytesRead
    var isKeyFrame = keyFrameInterval > 0
    var block = source.readGifBlock(decodeImages || isKeyFrame)
    while (block != GifTerminator) {
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
                ?: throw InvalidGifException("Frame $frameIndex has no color table")

                val keyframeArgb = if (decodeImages || isKeyFrame) {
                    val imageFrame = readImage(
                        canvasWidth,
                        canvasHeight,
                        block.descriptor,
                        block.colorIndices,
                        globalColorTableColors,
                        globalColorTable,
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
                        block.descriptor,
                        globalColorTableColors,
                        globalColorTable,
                        currentColorTable,
                        backgroundColorIndex,
                    )
                    if (disposedImage != null) {
                        previousImage = disposedImage
                    }

                    if (isKeyFrame) {
                        imageFrame.argb
                    } else {
                        EMPTY_INT_ARRAY
                    }
                } else {
                    EMPTY_INT_ARRAY
                }

                val frame = FrameInfo(
                    keyframeArgb,
                    canvasWidth,
                    canvasHeight,
                    duration,
                    timestamp,
                    frameIndex,
                    bytesRead,
                )
                frames.add(frame)

                // Reset values for next frame
                currentDisposalMethod = DisposalMethod.UNSPECIFIED
                currentDelayTime = 0
                currentTransparentColorIndex = -1

                frameIndex++
                timestamp += duration
            }

            else -> Unit
        }

        bytesRead = source.bytesRead
        isKeyFrame = keyFrameInterval > 0 && frameIndex % keyFrameInterval == 0
        block = source.readGifBlock(decodeImages || isKeyFrame)
    }

    return GifInfo(
        canvasWidth,
        canvasHeight,
        globalColorTable,
        backgroundColorIndex,
        frameIndex,
        timestamp,
        loopCount,
        frames,
    )
}

internal fun readImage(
    canvasWidth: Int,
    canvasHeight: Int,
    imageDescriptor: ImageDescriptor,
    colorIndices: ByteList,
    globalColorTableColors: Int,
    globalColorTable: ByteArray?,
    currentColorTable: ByteArray,
    backgroundColorIndex: Int,
    currentTransparentColorIndex: Int,
    previousImage: IntArray?,
    duration: Duration,
    timestamp: Duration,
    frameIndex: Int,
): ImageFrame {
    val left = imageDescriptor.left
    val top = imageDescriptor.top
    val width = imageDescriptor.width
    val height = imageDescriptor.height

    val canvasSize = canvasWidth * canvasHeight
    val image = IntArray(canvasSize) { i ->
        val absoluteX = i % canvasWidth
        val absoluteY = i / canvasWidth

        val relativeX = absoluteX - left
        val relativeY = absoluteY - top

        val colorIndex = run {
            if (relativeX in 0..<width && relativeY in 0..<height) {
                val index = relativeY * width + relativeX
                if (index in colorIndices.indices) {
                    return@run colorIndices[index].toUByte().toInt()
                }
            }
            // Missing indices are treated as transparent
            currentTransparentColorIndex
        }

        if (colorIndex == currentTransparentColorIndex) {
            val finalPreviousImage = previousImage
            if (finalPreviousImage == null) {
                if (
                    currentColorTable === globalColorTable
                    && backgroundColorIndex in 0..<globalColorTableColors
                ) {
                    getColor(globalColorTable, backgroundColorIndex)
                } else {
                    // Transparent
                    0
                }
            } else {
                finalPreviousImage[i]
            }
        } else {
            getColor(currentColorTable, colorIndex)
        }
    }

    return ImageFrame(
        image,
        canvasWidth,
        canvasHeight,
        duration,
        timestamp,
        frameIndex,
    )
}

internal fun disposeImage(
    image: IntArray,
    previousImage: IntArray?,
    disposalMethod: DisposalMethod,
    canvasWidth: Int,
    canvasHeight: Int,
    imageDescriptor: ImageDescriptor,
    globalColorTableColors: Int,
    globalColorTable: ByteArray?,
    currentColorTable: ByteArray,
    backgroundColorIndex: Int,
): IntArray? = when (disposalMethod) {
    DisposalMethod.UNSPECIFIED -> image
    DisposalMethod.DO_NOT_DISPOSE -> image
    DisposalMethod.RESTORE_TO_BACKGROUND_COLOR -> {
        if (previousImage == null) null
        else {
            val left = imageDescriptor.left
            val top = imageDescriptor.top
            val width = imageDescriptor.width
            val height = imageDescriptor.height

            val backgroundColor = if (
                currentColorTable === globalColorTable
                && backgroundColorIndex in 0..<globalColorTableColors
            ) {
                getColor(globalColorTable, backgroundColorIndex)
            } else {
                // Transparent
                0
            }

            val newPreviousImage = previousImage.copyOf()
            for (y in left..<height) {
                for (x in 0..<width) {
                    val absoluteX = left + x
                    val absoluteY = top + y
                    if (absoluteX >= canvasWidth || absoluteY >= canvasHeight) {
                        continue
                    }
                    val i = absoluteY * canvasWidth + absoluteX
                    newPreviousImage[i] = backgroundColor
                }
            }
            newPreviousImage
        }
    }

    DisposalMethod.RESTORE_TO_PREVIOUS -> null
}

/**
 * Used to identify which part of the GIF file caused an exception.
 */
internal inline fun <T> readGifSection(name: String, block: () -> T): T {
    try {
        return block()
    } catch (e: InvalidGifException) {
        throw e
    } catch (t: Throwable) {
        throw InvalidGifException("Failed to read GIF $name", t)
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
    if (exhausted()) return GifTerminator
    val blockIntroducer = readUByte().toInt()
    when (blockIntroducer) {
        0x21 -> readGifExtension()
        0x2C -> readGifImage(decodeImage)
        0x3B -> GifTerminator
        else -> throw InvalidGifException("Unknown block introducer: ${blockIntroducer.toHexByteString()}")
    }
}

private fun MonitoredSource.readGifExtension(): GifExtension = readGifSection("extension") {
    val extensionLabel = readUByte().toInt()
    when (extensionLabel) {
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

private fun MonitoredSource.readGifApplicationExtension(): ApplicationExtension =
    readGifSection("application extension") {
    val identifierSize = readUByte().toLong()
    val identifier = readString(identifierSize)
    when (identifier) {
        "NETSCAPE2.0" -> readGifNetscapeApplicationExtension()
        else -> readGifUnknownApplicationExtension(identifier)
    }
}

private fun MonitoredSource.readGifNetscapeApplicationExtension(): NetscapeApplicationExtension =
    readGifSection("Netscape application extension") {
        val data = readGifSubBlocks()
        val loopCountLow = data[1].toUByte().toInt()
        val loopCountHigh = data[2].toUByte().toInt()
        val loopCount = loopCountLow or (loopCountHigh shl 8)
        NetscapeApplicationExtension(loopCount)
    }

private fun MonitoredSource.readGifUnknownApplicationExtension(identifier: String): UnknownApplicationExtension =
    readGifSection("unknown application extension: $identifier") {
        skipGifSubBlocks()
        UnknownApplicationExtension
    }

private fun MonitoredSource.readGifCommentExtension(): CommentExtension = readGifSection("comment extension") {
    val comment = readGifSubBlocks().decodeToString()
    CommentExtension(comment)
}

private fun MonitoredSource.readGifUnknownExtension(label: Int): UnknownExtension =
    readGifSection("unknown extension: ${label.toHexByteString()}") {
        skipGifSubBlocks()
        UnknownExtension
    }

private fun MonitoredSource.readGifImage(
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
    var blockSize = readUByte().toInt()
    while (blockSize != 0) {
        repeat(blockSize) {
            data += readByte()
        }
        blockSize = readUByte().toInt()
    }
    return data
}

private fun MonitoredSource.skipGifSubBlocks() {
    var blockSize = readUByte().toLong()
    while (blockSize != 0L) {
        skip(blockSize)
        blockSize = readUByte().toLong()
    }
}

private fun getColor(colorTable: ByteArray, index: Int): Int {
    val colorIndex = index * BYTES_PER_COLOR
    val red = colorTable[colorIndex].toUByte().toInt()
    val green = colorTable[colorIndex + 1].toUByte().toInt()
    val blue = colorTable[colorIndex + 2].toUByte().toInt()
    val alpha = 0xFF
    return (alpha shl 24) or (red shl 16) or (green shl 8) or blue
}
