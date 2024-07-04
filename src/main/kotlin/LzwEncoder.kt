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

    val indexBuffer = mutableListOf<Byte>()
    indexBuffer.add(indexStream.first())

    indexStream.subList(1, indexStream.size).forEach { index ->
        val nextSequence = indexBuffer + index
        if (codeTable.containsKey(nextSequence)) {
            indexBuffer.add(index)
        } else {
            val outputCode = codeTable.getValue(indexBuffer)
            bitBuffer.writeBits(outputCode, codeSize)
            val buffer = bitBuffer.buffer
            while (buffer.size >= GIF_MAX_BLOCK_SIZE) {
                writeByte(GIF_MAX_BLOCK_SIZE)
                write(buffer, GIF_MAX_BLOCK_SIZE.toLong())
            }

            indexBuffer.clear()
            indexBuffer.add(index)

            val nextCode = codeTable.size + LZW_SPECIAL_CODES_COUNT
            if (nextCode > LZW_CODE_TABLE_MAX_CODE) {
                codeSize = initialCodeSize
                bitBuffer.writeBits(maxColors, codeSize) // Clear code
                initLzwCodeTable(codeTable, maxColors)
            } else {
                codeTable[nextSequence] = nextCode
                if (nextCode == 2.pow(codeSize)) {
                    codeSize++
                }
            }
        }
    }

    val code = codeTable.getValue(indexBuffer)
    bitBuffer.writeBits(code, codeSize)
    val endOfInformationCode = maxColors + 1
    bitBuffer.writeBits(endOfInformationCode, codeSize)
    bitBuffer.flush()

    val buffer = bitBuffer.buffer
    while (buffer.size > GIF_MAX_BLOCK_SIZE) {
        writeByte(GIF_MAX_BLOCK_SIZE)
        write(buffer, GIF_MAX_BLOCK_SIZE.toLong())
    }
    if (buffer.size > 0) {
        writeByte(buffer.size.toByte())
        transferFrom(buffer)
    }
    writeByte(0x00) // Block terminator
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
