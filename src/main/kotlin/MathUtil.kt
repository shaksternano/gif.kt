package io.github.shaksternano.gifcodec

import kotlin.math.pow

internal fun Int.pow(exponent: Int): Int =
    toDouble().pow(exponent).toInt()

internal fun Int.toLittleEndianShort(): Short {
    if (this == 0) {
        return 0
    }
    /*
     * No need to bit mask as the high byte is
     * truncated when converting to a Short
     */
    val low = this shl 8
    val high = this shr 8 and 0xFF
    return (low or high).toShort()
}

internal fun Int.roundUpPowerOf2(): Int {
    var result = this - 1
    result = result or (result shr 1)
    result = result or (result shr 2)
    result = result or (result shr 4)
    result = result or (result shr 8)
    result = result or (result shr 16)
    return result + 1
}
