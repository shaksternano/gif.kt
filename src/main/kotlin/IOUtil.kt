package io.github.shaksternano.gifcodec

import kotlinx.io.Sink

internal fun Sink.writeByte(byte: Int) =
    writeByte(byte.toByte())

internal fun Sink.writeLittleEndianShort(int: Int) {
    val lowHigh = int.toLittleEndianShort()
    writeShort(lowHigh)
}

internal fun Sink.writeLittleEndianShort(long: Long) =
    writeLittleEndianShort(long.toInt())
