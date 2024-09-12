package io.github.shaksternano.gifcodec

import kotlinx.io.Source

internal fun Source.readLittleEndianShort(): Int {
    val highLow = readShort()
    return highLow.toBigEndianInt()
}
