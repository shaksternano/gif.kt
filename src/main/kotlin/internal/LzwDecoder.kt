package io.github.shaksternano.gifcodec.internal

private const val MAX_LZW_CODE: Int = 0xFFF

internal fun MonitoredSource.readLzwIndexStream(): ByteList {
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
                val byte = block[i].toUByte().toInt()
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
                        endStream = true
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
                        if (codeTable.size < MAX_LZW_CODE) {
                            codeTable.add(nextSequence)
                            if (codeTable.size == 2.pow(currentCodeSize)) {
                                currentCodeSize++
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
        blockSize = readUByte().toInt()
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
