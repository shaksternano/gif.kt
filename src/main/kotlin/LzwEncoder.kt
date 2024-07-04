package io.github.shaksternano.gifcodec

import kotlinx.io.Sink

private const val GIF_MAX_BLOCK_SIZE: Int = 0xFF
private const val LZW_SPECIAL_CODES_COUNT: Int = 2
private const val LZW_CODE_TABLE_MAX_CODE: Int = 4095

/*
 * Reference:
 * https://www.matthewflickinger.com/lab/whatsinagif/lzw_image_data.asp
 */
fun Sink.writeLzwIndexStream(indexStream: List<Byte>, maxColors: Int) {
    if (indexStream.isEmpty()) {
        throw IllegalArgumentException("Index stream is empty")
    }

    val lzwMinCodeSize = requiredBits(maxColors)
    writeByte(lzwMinCodeSize)

    val codeTable = mutableMapOf<List<Byte>, Int>()
    initLzwCodeTable(codeTable, maxColors)
    val initialCodeSize = lzwMinCodeSize + 1
    var codeSize = initialCodeSize

    val bitBuffer = BitBuffer()
    bitBuffer.writeBits(maxColors, codeSize) // Clear code
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
                bitBuffer.writeBits(maxColors, codeSize) // Clear code
                tryWriteFullLzwSubBlock(bitBuffer)
                initLzwCodeTable(codeTable, maxColors)
                codeSize = initialCodeSize
            } else if (nextCode == 2.pow(codeSize)) {
                codeSize++
            }
        }
    }

    val code = codeTable.getValue(indexBuffer)
    bitBuffer.writeBits(code, codeSize)
    val endOfInformationCode = maxColors + 1
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
    maxColors: Int,
) {
    codeTable.clear()
    repeat(maxColors) { i ->
        codeTable[listOf(i.toByte())] = i
    }
}

private fun requiredBits(values: Int): Int =
    Int.SIZE_BITS - (values - 1).countLeadingZeroBits()
