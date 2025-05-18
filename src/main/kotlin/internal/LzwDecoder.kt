package io.github.shaksternano.gifcodec.internal

import kotlinx.io.Source
import kotlinx.io.readUByte
import kotlin.properties.Delegates

private const val MAX_LZW_CODE: Int = 0xFFF

internal fun Source.readLzwIndexStream(): ByteList {
    val lzwMinCodeSize = readUByte().toInt()
    val clearCode = 2 shl (lzwMinCodeSize - 1)
    val endOfInformationCode = clearCode + 1

    // Subblock byte count
    var blockSize = readUByte().toInt()
    val initialCodeSize = lzwMinCodeSize + 1
    var currentCodeSize = initialCodeSize
    var currentBits = 0
    var currentBitPosition = 0

    var reset = true
    var previousCode by Delegates.notNull<Int>()

    val codeTable = mutableListOf<ByteList>()
    val indexStream = ByteList()
    while (blockSize > 0) {
        repeat(blockSize) {
            val byte = readUByte().toInt()
            currentBits = currentBits or (byte shl currentBitPosition)
            currentBitPosition += Byte.SIZE_BITS
            while (currentBitPosition >= currentCodeSize) {
                // Extract the required number of bits
                val mask = (1 shl currentCodeSize) - 1
                val code = currentBits and mask

                currentBits = currentBits ushr currentCodeSize
                currentBitPosition -= currentCodeSize

                if (code == clearCode) {
                    currentCodeSize = initialCodeSize
                    reset = true
                } else if (code == endOfInformationCode) {
                    return@repeat
                } else if (reset) {
                    initCodeTable(codeTable, clearCode)
                    val indices = codeTable[code]
                    indexStream.addAll(indices)
                    previousCode = code
                    reset = false
                } else {
                    val indices = codeTable.getOrNull(code)
                    val previousIndices = codeTable[previousCode]
                    val nextSequence = if (indices == null) {
                        val firstIndex = previousIndices.first()
                        val nextSequence = previousIndices + firstIndex
                        indexStream.addAll(nextSequence)
                        nextSequence
                    } else {
                        indexStream.addAll(indices)
                        val firstIndex = indices.first()
                        previousIndices + firstIndex
                    }
                    if (codeTable.size < MAX_LZW_CODE) {
                        codeTable.add(nextSequence)
                        if (codeTable.size == 2.pow(currentCodeSize)) {
                            currentCodeSize++
                        }
                    }
                    previousCode = code
                }
            }
        }
        blockSize = readUByte().toInt()
    }
    return indexStream
}

private fun initCodeTable(codeTable: MutableList<ByteList>, clearCode: Int) {
    codeTable.clear()
    repeat(clearCode) { i ->
        codeTable.add(ByteList(i.toByte()))
    }
    codeTable.add(ByteList()) // Clear code
    codeTable.add(ByteList()) // End of information code
}
