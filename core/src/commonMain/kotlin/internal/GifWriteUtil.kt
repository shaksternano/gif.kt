package com.shakster.gifkt.internal

import com.shakster.gifkt.ColorDistanceCalculator
import com.shakster.gifkt.ColorQuantizer
import kotlinx.io.Sink
import kotlinx.io.writeShortLe
import kotlinx.io.writeString
import kotlin.math.ceil
import kotlin.math.log2

const val GIF_MAX_COLORS: Int = 256
const val GIF_MINIMUM_FRAME_DURATION_CENTISECONDS: Int = 2
internal const val GIF_MINIMUM_COLOR_TABLE_SIZE: Int = 2
internal const val GIF_MAX_BLOCK_SIZE: Int = 0xFF

@PublishedApi
internal fun optimizeTransparency(
    previousImage: Image,
    currentImage: Image,
    colorTolerance: Double,
    colorDistanceCalculator: ColorDistanceCalculator,
    safeTransparent: Boolean,
): Image? {
    if (previousImage.width != currentImage.width
        || previousImage.height != currentImage.height
        || previousImage.argb.size != currentImage.argb.size
    ) {
        return null
    }
    val optimizedPixels = IntArray(currentImage.argb.size) { i ->
        val previousArgb = previousImage.argb[i]
        val currentArgb = currentImage.argb[i]
        if (previousArgb == currentArgb) {
            return@IntArray 0
        }
        val previousAlpha = previousArgb ushr 24
        val currentAlpha = currentArgb ushr 24
        // Previous is opaque, current is transparent
        if (previousAlpha != 0 && currentAlpha == 0) {
            if (safeTransparent) {
                return@IntArray 0
            } else {
                return null
            }
        }
        if (colorTolerance == 0.0) {
            return@IntArray currentArgb
        }
        // Previous is transparent, current is opaque
        if (previousAlpha == 0 && currentAlpha != 0) {
            return@IntArray currentArgb
        }
        val colorDistance = colorDistanceCalculator.colorDistance(previousArgb, currentArgb)
        if (colorDistance > colorTolerance) {
            currentArgb
        } else {
            0
        }
    }
    return Image(optimizedPixels, currentImage.width, currentImage.height)
}

fun getImageData(
    image: Image,
    maxColors: Int,
    quantizer: ColorQuantizer,
    forceTransparency: Boolean,
): QuantizedImageData {
    // Build color table
    val (argb, width, height) = image
    val rgb = mutableListOf<Byte>()
    val distinctColors = hashSetOf<Int>()
    var hasTransparent = forceTransparency
    argb.forEach { pixel ->
        val alpha = pixel ushr 24
        if (alpha == 0) {
            hasTransparent = true
        } else {
            val red = pixel shr 16 and 0xFF
            val green = pixel shr 8 and 0xFF
            val blue = pixel and 0xFF
            rgb.add(red.toByte())
            rgb.add(green.toByte())
            rgb.add(blue.toByte())
            distinctColors.add(pixel)
        }
    }
    val distinctColorCount = distinctColors.size + if (hasTransparent) 1 else 0
    val colorCount = distinctColorCount.coerceAtMost(maxColors)
    val colorTableSize = colorCount.roundUpPowerOf2()
        .coerceAtLeast(GIF_MINIMUM_COLOR_TABLE_SIZE)
    val actualQuantizer = if (distinctColorCount > maxColors) {
        quantizer
    } else {
        DirectColorQuantizer
    }
    val quantizerMaxColors =
        if (hasTransparent) colorCount - 1
        else colorCount
    val quantizationResult = actualQuantizer.quantize(
        rgb.toByteArray(),
        quantizerMaxColors,
    )
    val quantizedColors = quantizationResult.colors
    val colorTableBytes = colorTableSize * 3
    val colorTable: ByteArray
    val transparentColorIndex: Int
    if (hasTransparent) {
        colorTable = ByteArray(colorTableBytes)
        quantizedColors.copyInto(
            colorTable,
            // The first three bytes are reserved for transparent color
            destinationOffset = 3,
        )
        transparentColorIndex = 0
    } else {
        if (quantizedColors.size == colorTableBytes) {
            colorTable = quantizedColors
        } else {
            colorTable = ByteArray(colorTableBytes)
            quantizedColors.copyInto(colorTable)
        }
        transparentColorIndex = -1
    }

    // Get color indices
    val imageColorIndices = ByteArray(argb.size)
    // The first index is reserved for transparent color
    val indexOffset = if (hasTransparent) 1 else 0
    argb.forEachIndexed { i, pixel ->
        val index = if (colorCount == 1) {
            0
        } else {
            val alpha = pixel ushr 24
            if (alpha == 0) {
                0
            } else {
                val red = pixel shr 16 and 0xFF
                val green = pixel shr 8 and 0xFF
                val blue = pixel and 0xFF
                quantizationResult.getColorIndex(red, green, blue) + indexOffset
            }
        }
        imageColorIndices[i] = index.toByte()
    }

    return QuantizedImageData(
        imageColorIndices,
        width,
        height,
        x = 0,
        y = 0,
        colorTable,
        transparentColorIndex,
    )
}

@PublishedApi
internal fun QuantizedImageData.cropTransparentBorder(): QuantizedImageData {
    if (transparentColorIndex < 0) {
        return this
    }

    val (x, y, width, height) = opaqueArea()
    return crop(x, y, width, height)
}

@PublishedApi
internal fun QuantizedImageData.opaqueArea(): Rectangle {
    if (transparentColorIndex < 0) {
        return bounds
    }
    val transparentIndex = transparentColorIndex.toByte()

    var startX = Int.MAX_VALUE
    var startY = -1

    var endX = 0
    var endY = 0

    var x = 0
    var y = 0
    imageColorIndices.forEach { index ->
        if (index != transparentIndex) {
            if (x < startX) {
                startX = x
            }
            if (startY == -1) {
                startY = y
            }
            if (x > endX) {
                endX = x
            }
            endY = y
        }

        x++
        if (x == width) {
            x = 0
            y++
        }
    }

    if (startX == Int.MAX_VALUE) {
        return Rectangle(0, 0, 0, 0)
    }

    val newWidth = endX - startX + 1
    val newHeight = endY - startY + 1

    return Rectangle(startX, startY, newWidth, newHeight)
}

@PublishedApi
internal fun QuantizedImageData.crop(
    startX: Int,
    startY: Int,
    newWidth: Int,
    newHeight: Int
): QuantizedImageData {
    if (startX == 0 && startY == 0 && newWidth == width && newHeight == height) {
        return this
    }

    val croppedIndices = if (width == newWidth && height == newHeight) {
        imageColorIndices
    } else if (width == newWidth) {
        imageColorIndices.copyOfRange(startY * newWidth, (startY + newHeight) * newWidth)
    } else {
        var x = 0
        var y = 0
        ByteArray(newWidth * newHeight) {
            val oldX = startX + x
            val oldY = startY + y
            val i = oldX + oldY * width

            x++
            if (x == newWidth) {
                x = 0
                y++
            }

            imageColorIndices[i]
        }
    }

    return copy(
        imageColorIndices = croppedIndices,
        width = newWidth,
        height = newHeight,
        x = startX,
        y = startY,
    )
}

internal fun Sink.writeByte(byte: Int) =
    writeByte(byte.toByte())

private fun Sink.writeLittleEndianShort(int: Int) {
    writeShortLe(int.toShort())
}

@PublishedApi
internal fun Sink.writeGifIntro(
    width: Int,
    height: Int,
    loopCount: Int,
    comment: String,
) {
    writeGifHeader()
    writeGifLogicalScreenDescriptor(width, height)
    writeGifApplicationExtension(loopCount)
    writeGifCommentExtension(comment)
}

internal fun Sink.writeGifHeader() {
    writeString("GIF89a")
}

internal fun Sink.writeGifLogicalScreenDescriptor(width: Int, height: Int) {
    writeLittleEndianShort(width)
    writeLittleEndianShort(height)
    writeByte(0x00) // Global color table packed field
    writeByte(0x00) // Background color index
    writeByte(0x00) // Pixel aspect ratio
}

internal fun Sink.writeGifColorTable(colorTable: ByteArray) {
    write(colorTable)
}

internal fun Sink.writeGifApplicationExtension(loopCount: Int) {
    // No looping
    if (loopCount < 0) {
        return
    }

    writeByte(0x21)            // Extension introducer
    writeByte(0xFF)            // Application extension label
    writeByte(0x0B)            // Length of Application block, 11 bytes
    writeString("NETSCAPE2.0") // Application identifier
    writeByte(0x03)            // Length of data subblock, 3 bytes
    writeByte(0x01)            // Constant
    writeLittleEndianShort(loopCount)
    writeByte(0x00)            // Block Terminator
}

internal fun Sink.writeGifCommentExtension(comment: String) {
    if (comment.isEmpty()) {
        return
    }

    writeByte(0x21) // Extension introducer
    writeByte(0xFE) // Comment extension label
    writeGifSubBlocks(comment.encodeToByteArray())
    writeByte(0x00) // Block Terminator
}

fun Sink.writeGifImage(
    data: QuantizedImageData,
    durationCentiseconds: Int,
    disposalMethod: DisposalMethod,
) {
    val (
        imageColorIndices,
        width,
        height,
        x,
        y,
        colorTable,
        transparentColorIndex,
    ) = data
    val colorTableSize = colorTable.size / 3
    writeGifGraphicsControlExtension(disposalMethod, durationCentiseconds, transparentColorIndex)
    writeGifImageDescriptor(
        width,
        height,
        x,
        y,
        colorTableSize,
    )
    writeGifColorTable(colorTable)
    writeGifImageData(imageColorIndices, colorTableSize)
}

internal fun Sink.writeGifGraphicsControlExtension(
    disposalMethod: DisposalMethod,
    durationCentiseconds: Int,
    transparentColorIndex: Int,
) {
    writeByte(0x21) // Extension code
    writeByte(0xF9) // Graphics Control Label
    writeByte(0x04) // Block size
    val transparentColorFlag = if (transparentColorIndex < 0) 0 else 1
    /*
     * Bits:
     * 1-3 : Reserved for future use (unused, set to 0)
     * 4-6 : Disposal method
     * 7   : User input flag         (unused, set to 0)
     * 8   : Transparent color flag
     */
    val packed = (disposalMethod.id shl 2 or transparentColorFlag) and 0b00011101
    writeByte(packed)
    writeLittleEndianShort(durationCentiseconds)
    writeByte(transparentColorIndex.coerceAtLeast(0))
    writeByte(0x00) // Block terminator
}

internal fun Sink.writeGifImageDescriptor(
    width: Int,
    height: Int,
    x: Int,
    y: Int,
    localColorTableSize: Int,
) {
    writeByte(0x2C) // Image Separator
    writeLittleEndianShort(x)
    writeLittleEndianShort(y)
    writeLittleEndianShort(width)
    writeLittleEndianShort(height)
    /*
     * Bits:
     * 1   : Local color table flag
     * 2   : Interlace flag          (unused, set to 0)
     * 3   : Sort flag               (unused, set to 0)
     * 4-5 : Reserved for future use (unused, set to 0)
     * 6-8 : Local color table size
     */
    val packed = (0b10000000 or getColorTableRepresentedSize(localColorTableSize)) and 0b10000111
    writeByte(packed)
}

internal fun Sink.writeGifImageData(imageColorIndices: ByteArray, colorTableSize: Int) {
    writeLzwIndexStream(imageColorIndices, colorTableSize)
}

@PublishedApi
internal fun Sink.writeGifTrailer() {
    writeByte(0x3B)
}

private fun Sink.writeGifSubBlocks(bytes: ByteArray) {
    bytes.asList()
        .chunked(GIF_MAX_BLOCK_SIZE)
        .forEach {
            writeByte(it.size)      // Number of bytes of data in subblock
            write(it.toByteArray()) // Sub-block data
        }
}

private fun getColorTableRepresentedSize(maxColors: Int): Int =
    ceil(log2(maxColors.toDouble())).toInt() - 1
