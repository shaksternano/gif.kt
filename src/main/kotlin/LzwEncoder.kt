package io.github.shaksternano.gifcodec

private const val LZW_SPECIAL_CODES_COUNT: Int = 2
private const val LZW_CODE_TABLE_MAX_CODE: Int = 4095

data class LzwCode(
    val code: Int,
    val codeSize: Int,
)

/*
 * Reference:
 * https://www.matthewflickinger.com/lab/whatsinagif/lzw_image_data.asp
 */
fun lzwEncode(imageColorIndices: ByteArray, maxColors: Int): List<LzwCode> {
    val codeTable = mutableMapOf<List<Byte>, Int>()
    initLzwCodeTable(codeTable, maxColors)
    val codeStream = mutableListOf<LzwCode>()
    val initialCodeSize = requiredBits(maxColors) + 1
    var codeSize = initialCodeSize
    codeStream.add(LzwCode(maxColors, codeSize)) // Clear code
    if (imageColorIndices.isEmpty()) {
        return codeStream
    }

    val indexBuffer = mutableListOf<Byte>()
    indexBuffer.add(imageColorIndices.first())

    imageColorIndices.forEachIndexed { i, nextIndex ->
        // We've already added the first index to the buffer
        if (i == 0) {
            return@forEachIndexed
        }

        val nextSequence = indexBuffer + nextIndex
        if (codeTable.containsKey(nextSequence)) {
            indexBuffer.add(nextIndex)
        } else {
            codeStream.add(LzwCode(codeTable.getValue(indexBuffer), codeSize))
            indexBuffer.clear()
            indexBuffer.add(nextIndex)

            // Take the clear and end of information codes into account
            val nextCode = codeTable.size + LZW_SPECIAL_CODES_COUNT
            if (nextCode > LZW_CODE_TABLE_MAX_CODE) {
                codeSize = initialCodeSize
                codeStream.add(LzwCode(maxColors, codeSize)) // Clear code
                initLzwCodeTable(codeTable, maxColors)
            } else {
                codeTable[nextSequence] = nextCode
                if (nextCode == 2.pow(codeSize)) {
                    codeSize++
                }
            }
        }
    }

    codeStream.add(LzwCode(codeTable.getValue(indexBuffer), codeSize))
    codeStream.add(LzwCode(maxColors + 1, codeSize)) // End of information code
    return codeStream
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

fun requiredBits(values: Int): Int =
    Int.SIZE_BITS - (values - 1).countLeadingZeroBits()
