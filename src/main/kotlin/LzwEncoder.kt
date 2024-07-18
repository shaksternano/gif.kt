package io.github.shaksternano.gifcodec

import kotlinx.io.Sink

private const val LZW_SPECIAL_CODES_COUNT: Int = 2
private const val LZW_CODE_TABLE_MAX_CODE: Int = 4095
private const val LZW_MINIMUM_COLORS: Int = 4

/*
 * Reference:
 * https://www.matthewflickinger.com/lab/whatsinagif/lzw_image_data.asp
 */
internal fun Sink.writeLzwIndexStream(indexStream: List<Byte>, colorTableSize: Int) {
    if (indexStream.isEmpty()) {
        throw IllegalArgumentException("Index stream is empty")
    }

    val colorCount = colorTableSize.coerceAtLeast(LZW_MINIMUM_COLORS)
    val lzwMinCodeSize = requiredBits(colorCount)
    writeByte(lzwMinCodeSize)

    val codeTable = mutableMapOf<List<Byte>, Int>()
    initLzwCodeTable(codeTable, colorCount)
    val initialCodeSize = lzwMinCodeSize + 1
    var codeSize = initialCodeSize

    val bitBuffer = BitBuffer()
    bitBuffer.writeBits(colorCount, codeSize) // Clear code
    tryWriteFullLzwSubBlock(bitBuffer)

    val indexBuffer = mutableListOf<Byte>()
    indexBuffer.add(indexStream.first())

    indexStream.subList(1, indexStream.size).forEach { index ->
        val nextSequence = indexBuffer + index
        if (codeTable.containsKey(nextSequence)) {
            indexBuffer.add(index)
        } else {
            val nextCode = codeTable.size + LZW_SPECIAL_CODES_COUNT
            codeTable[nextSequence] = nextCode

            val outputCode = codeTable.getValue(indexBuffer)
            bitBuffer.writeBits(outputCode, codeSize)
            tryWriteFullLzwSubBlock(bitBuffer)

            indexBuffer.clear()
            indexBuffer.add(index)

            val nextNextCode = codeTable.size + LZW_SPECIAL_CODES_COUNT
            if (nextNextCode > LZW_CODE_TABLE_MAX_CODE) {
                // Rebuild the code table if the maximum code is reached
                bitBuffer.writeBits(colorCount, codeSize) // Clear code
                tryWriteFullLzwSubBlock(bitBuffer)
                initLzwCodeTable(codeTable, colorCount)
                codeSize = initialCodeSize
            } else if (nextCode == 2.pow(codeSize)) {
                codeSize++
            }
        }
    }

    val code = codeTable.getValue(indexBuffer)
    bitBuffer.writeBits(code, codeSize)
    val endOfInformationCode = colorCount + 1
    bitBuffer.writeBits(endOfInformationCode, codeSize)
    bitBuffer.flush()

    tryWriteFullLzwSubBlock(bitBuffer)
    val buffer = bitBuffer.buffer
    if (buffer.size > 0) {
        writeByte(buffer.size.toByte())
        transferFrom(buffer)
    }
    writeByte(0x00) // Block terminator
}

private fun Sink.tryWriteFullLzwSubBlock(bitBuffer: BitBuffer) {
    val buffer = bitBuffer.buffer
    if (buffer.size >= GIF_MAX_BLOCK_SIZE) {
        writeByte(GIF_MAX_BLOCK_SIZE)
        write(buffer, GIF_MAX_BLOCK_SIZE.toLong())
    }
}

private fun initLzwCodeTable(
    codeTable: MutableMap<List<Byte>, Int>,
    colorTableSize: Int,
) {
    codeTable.clear()
    repeat(colorTableSize) { i ->
        codeTable[listOf(i.toByte())] = i
    }
}

private fun requiredBits(values: Int): Int =
    Int.SIZE_BITS - (values - 1).countLeadingZeroBits()
