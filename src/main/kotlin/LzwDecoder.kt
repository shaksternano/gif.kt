package io.github.shaksternano.gifcodec

fun lzwDecode(codeStream: List<Int>, maxColors: Int): List<Byte> {
    if (codeStream.size < 2) {
        return emptyList()
    }

    val indexStream = mutableListOf<Byte>()
    val clearCode = codeStream.first()
    val endOfInformationCode = clearCode + 1
    val splitCodeStream = mutableListOf<List<Int>>()
    var currentSubCodeStream = mutableListOf<Int>()
    codeStream.forEach { code ->
        if (code == clearCode) {
            if (currentSubCodeStream.isNotEmpty()) {
                splitCodeStream.add(currentSubCodeStream)
                currentSubCodeStream = mutableListOf()
            }
        } else if (code != endOfInformationCode) {
            currentSubCodeStream.add(code)
        }
    }
    splitCodeStream.add(currentSubCodeStream)

    val codeTable = mutableListOf<List<Byte>>()
    splitCodeStream.forEach { subCodeStream ->
        initCodeTable(codeTable, maxColors)
        val firstCode = subCodeStream.first()
        indexStream.addAll(codeTable[firstCode])

        var previousCode = firstCode
        subCodeStream.forEachIndexed { i, code ->
            // We've already visited the first code
            if (i == 0) {
                return@forEachIndexed
            }

            val indices = codeTable.getOrNull(code)
            if (indices == null) {
                val previousCodeValue = codeTable[previousCode]
                val firstIndex = previousCodeValue.first()
                val newSequence = previousCodeValue + firstIndex
                indexStream.addAll(newSequence)
                codeTable.add(newSequence)
            } else {
                indexStream.addAll(indices)
                val previousCodeValue = codeTable[previousCode]
                val firstIndex = indices.first()
                val newSequence = previousCodeValue + firstIndex
                codeTable.add(newSequence)
            }
            previousCode = code
        }
    }

    return indexStream
}

fun initCodeTable(codeTable: MutableList<List<Byte>>, maxColors: Int) {
    codeTable.clear()
    repeat(maxColors) { i ->
        codeTable.add(listOf(i.toByte()))
    }
    codeTable.add(emptyList()) // Clear code
    codeTable.add(emptyList()) // End of information code
}
