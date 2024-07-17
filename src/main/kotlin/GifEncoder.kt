package io.github.shaksternano.gifcodec

import kotlinx.io.Sink
import kotlinx.io.writeString
import kotlin.math.ceil
import kotlin.math.log2
import kotlin.math.pow
import kotlin.time.Duration

private const val GIF_MAX_COLORS: Int = 256
internal const val GIF_MAX_BLOCK_SIZE: Int = 0xFF

/*
 * Reference:
 * https://www.matthewflickinger.com/lab/whatsinagif/bits_and_bytes.asp
 */
class GifEncoder(
    private val sink: Sink,
    private val loopCount: Int = 0,
    private val comment: String = "",
) : AutoCloseable {

    private var initialized: Boolean = false

    fun writeFrame(
        image: IntArray,
        width: Int,
        height: Int,
        delay: Duration,
        disposalMethod: DisposalMethod,
    ) {
        init(width, height)

        // Build color table
        val bgr = mutableListOf<Byte>()
        var hasTransparent = false
        image.forEachIndexed { i, pixel ->
            val alpha = pixel shr 24 and 0xFF
            val red = pixel shr 16 and 0xFF
            val green = pixel shr 8 and 0xFF
            val blue = pixel and 0xFF
            if (alpha == 0) {
                hasTransparent = true
            } else {
                bgr.add(blue.toByte())
                bgr.add(green.toByte())
                bgr.add(red.toByte())
            }
        }
        val maxColors =
            if (hasTransparent) GIF_MAX_COLORS - 1
            else GIF_MAX_COLORS
        val neuQuant = NeuQuant(
            image = bgr.toByteArray(),
            maxColors = maxColors,
        )
        val quantizationResult = neuQuant.process()
        val colorTable: ByteArray
        val transparentColorIndex: Int
        if (hasTransparent) {
            colorTable = ByteArray(quantizationResult.size + 3)
            quantizationResult.copyInto(
                colorTable,
                // First three bytes are reserved for transparent color
                destinationOffset = 3,
            )
            transparentColorIndex = 0
        } else {
            colorTable = quantizationResult
            transparentColorIndex = -1
        }

        // Get color indices
        val imageColorIndices = ByteArray(image.size)
        // First index is reserved for transparent color
        val indexOffset = if (hasTransparent) 1 else 0
        image.forEachIndexed { i, pixel ->
            val alpha = pixel shr 24 and 0xFF
            val red = pixel shr 16 and 0xFF
            val green = pixel shr 8 and 0xFF
            val blue = pixel and 0xFF
            val index = if (alpha == 0) {
                0
            } else {
                neuQuant.map(blue, green, red) + indexOffset
            }
            imageColorIndices[i] = index.toByte()
        }

        sink.writeGifGraphicsControlExtension(disposalMethod, delay, transparentColorIndex)
        sink.writeGifImageDescriptor(width, height, GIF_MAX_COLORS)
        sink.writeGifColorTable(colorTable)
        sink.writeGifImageData(imageColorIndices, GIF_MAX_COLORS)
    }

    private fun init(width: Int, height: Int) {
        if (initialized) return
        sink.writeGifHeader()
        sink.writeGifLogicalScreenDescriptor(width, height)
        sink.writeGifApplicationExtension(loopCount)
        sink.writeGifCommentExtension(comment)
        initialized = true
    }

    override fun close() {
        sink.writeGifTrailer()
        sink.close()
    }
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

internal fun Sink.writeGifGraphicsControlExtension(
    disposalMethod: DisposalMethod,
    delay: Duration,
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
    writeLittleEndianShort(delay.inWholeMilliseconds / 10)
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

internal fun Sink.writeGifImageData(imageColorIndices: ByteArray, maxColors: Int) {
    writeLzwIndexStream(imageColorIndices.asList(), maxColors)
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

private fun Sink.writeLittleEndianShort(int: Int) {
    val lowHigh = int.toLittleEndianShort()
    writeShort(lowHigh)
}

private fun Sink.writeLittleEndianShort(long: Long) =
    writeLittleEndianShort(long.toInt())

private fun Int.toLittleEndianShort(): Short {
    /*
     * No need to bit mask as the high byte is
     * truncated when converting to a Short
     */
    val low = this shl 8
    val high = this shr 8 and 0xFF
    return (low or high).toShort()
}

fun Int.pow(exponent: Int): Int =
    toDouble().pow(exponent).toInt()
