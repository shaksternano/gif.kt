package com.shakster.gifkt.internal

private const val MAX_LZW_CODE: Int = 4095

internal fun MonitoredSource.readLzwIndexStream(): ByteList {
    val lzwMinCodeSize = readByte().toInt() and 0xFF
    val clearCode = 2 shl (lzwMinCodeSize - 1)
    val endOfInformationCode = clearCode + 1

    // Subblock byte count
    var blockSize = readByte().toInt() and 0xFF
    val initialCodeSize = lzwMinCodeSize + 1
    var currentCodeSize = initialCodeSize
    var growCode = 2.pow(currentCodeSize)
    var currentBits = 0
    var currentBitPosition = 0

    var reset = true
    var previousCode = -1
    var endStream = false

    val codeTable = mutableListOf<ByteList>()
    val indexStream = ByteList()
    while (blockSize > 0) {
        if (endStream) {
            skip(blockSize.toLong())
        } else {
            val block = readByteArray(blockSize)
            for (i in 0..<blockSize) {
                val byte = block[i].toInt() and 0xFF
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
                        growCode = 2.pow(currentCodeSize)
                        reset = true
                    } else if (code == endOfInformationCode) {
                        endStream = true
                        break
                    } else if (reset) {
                        initCodeTable(codeTable, clearCode)
                        val indices = codeTable[code]
                        indexStream += indices
                        previousCode = code
                        reset = false
                    } else {
                        val indices = codeTable.getOrNull(code)
                        val previousIndices = codeTable[previousCode]
                        val nextSequence = if (indices == null) {
                            val firstIndex = previousIndices.first()
                            val nextSequence = previousIndices + firstIndex
                            indexStream += nextSequence
                            nextSequence
                        } else {
                            indexStream += indices
                            val firstIndex = indices.first()
                            previousIndices + firstIndex
                        }
                        if (codeTable.size <= MAX_LZW_CODE) {
                            codeTable.add(nextSequence)
                            if (codeTable.size == growCode && codeTable.size <= MAX_LZW_CODE) {
                                currentCodeSize++
                                growCode = 2.pow(currentCodeSize)
                            }
                        }
                        previousCode = code
                    }
                }

                if (endStream) {
                    break
                }
            }
        }
        blockSize = readByte().toInt() and 0xFF
    }
    return indexStream
}

private fun initCodeTable(codeTable: MutableList<ByteList>, clearCode: Int) {
    codeTable.clear()
    for (i in 0..<clearCode) {
        codeTable.add(ByteList(i.toByte()))
    }
    codeTable.add(ByteList()) // Clear code
    codeTable.add(ByteList()) // End of information code
}
