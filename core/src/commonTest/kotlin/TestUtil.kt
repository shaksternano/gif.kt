package com.shakster.gifkt

import kotlinx.io.Buffer
import kotlinx.io.readByteArray

fun Buffer.readByteList(): HexByteList =
    readByteArray().map { it.toInt() and 0xFF }.asHexByteList()
