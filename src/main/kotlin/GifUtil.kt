package io.github.shaksternano.gifcodec

import kotlinx.io.Sink
import kotlinx.io.writeString
import kotlin.math.ceil
import kotlin.math.log2
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

internal const val GIF_MAX_COLORS: Int = 256
internal const val GIF_MINIMUM_COLOR_TABLE_SIZE: Int = 2
internal const val GIF_MINIMUM_FRAME_DURATION_CENTISECONDS: Int = 2
internal const val GIF_MAX_BLOCK_SIZE: Int = 0xFF

internal val Int.centiseconds: Duration
    get() = (this * 10).milliseconds

internal val Duration.roundedUpCentiseconds: Int
    get() = ceil(inWholeMilliseconds / 10.0).toInt()

@Suppress("ArrayInDataClass")
internal data class QuantizedImageData(
    val colorTable: ByteArray,
    val imageColorIndices: ByteArray,
    val transparentColorIndex: Int,
)

internal fun optimizeTransparency(
    previousImage: Image,
    currentImage: Image,
    colorTolerance: Double,
): Image? {
    if (previousImage.width != currentImage.width || previousImage.height != currentImage.height) {
        return null
    }
    val optimizedPixels = IntArray(currentImage.argb.size)
    previousImage.argb.zip(currentImage.argb).forEachIndexed { i, (previousArgb, currentArgb) ->
        val previousAlpha = previousArgb ushr 24
        val currentAlpha = currentArgb ushr 24
        if (currentAlpha == 0 && previousAlpha != 0) {
            /*
             * Current frame has a transparent pixel where
             * the previous frame had an opaque pixel
             */
            return null
        }
        val colorDistance = colorDistance(previousArgb, currentArgb)
        optimizedPixels[i] = if (colorDistance > colorTolerance) {
            currentArgb
        } else {
            0
        }
    }
    return Image(optimizedPixels, currentImage.width, currentImage.height)
}

internal fun getImageData(argb: IntArray, maxColors: Int, quantizer: ColorQuantizer): QuantizedImageData {
    // Build color table
    val rgb = mutableListOf<Byte>()
    val distinctColors = mutableSetOf<Int>()
    var hasTransparent = false
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
            // First three bytes are reserved for transparent color
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
    // First index is reserved for transparent color
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
        colorTable,
        imageColorIndices,
        transparentColorIndex,
    )
}

internal fun Sink.writeByte(byte: Int) =
    writeByte(byte.toByte())

private fun Sink.writeLittleEndianShort(int: Int) {
    val lowHigh = int.toLittleEndianShort()
    writeShort(lowHigh)
}

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
    writeByte(0x03)            // Length of data sub-block, 3 bytes
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

internal fun Sink.writeGifImage(
    data: QuantizedImageData,
    width: Int,
    height: Int,
    durationCentiseconds: Int,
    disposalMethod: DisposalMethod,
) {
    val (colorTable, imageColorIndices, transparentColorIndex) = data
    val colorTableSize = colorTable.size / 3
    writeGifGraphicsControlExtension(disposalMethod, durationCentiseconds, transparentColorIndex)
    writeGifImageDescriptor(width, height, colorTableSize)
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

internal fun Sink.writeGifImageDescriptor(width: Int, height: Int, localColorTableSize: Int) {
    writeByte(0x2C) // Image Separator
    writeInt(0)     // Image left and top positions, two bytes each
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
    writeLzwIndexStream(imageColorIndices.asList(), colorTableSize)
}

internal fun Sink.writeGifTrailer() {
    writeByte(0x3B)
}

private fun Sink.writeGifSubBlocks(bytes: ByteArray) {
    bytes.asList()
        .chunked(GIF_MAX_BLOCK_SIZE)
        .forEach {
            writeByte(it.size)      // Number of bytes of data in sub-block
            write(it.toByteArray()) // Sub-block data
        }
}

private fun getColorTableRepresentedSize(maxColors: Int): Int =
    ceil(log2(maxColors.toDouble())).toInt() - 1
