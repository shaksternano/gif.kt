package io.github.shaksternano.gifcodec

import kotlinx.io.Buffer

internal class BitBuffer {

    val buffer: Buffer = Buffer()
    private var currentBits: Int = 0
    private var currentBitPosition: Int = 0

    fun writeBits(bits: Int, count: Int) {
        currentBits = currentBits or (bits shl currentBitPosition)
        currentBitPosition += count
        while (currentBitPosition >= Byte.SIZE_BITS) {
            buffer.writeByte(currentBits)
            currentBits = currentBits ushr Byte.SIZE_BITS
            currentBitPosition -= Byte.SIZE_BITS
        }
    }

    fun flush() {
        if (currentBitPosition != 0) {
            buffer.writeByte(currentBits)
            currentBits = 0
            currentBitPosition = 0
        }
    }
}
