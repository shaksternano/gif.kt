package io.github.shaksternano.gifcodec

import kotlin.math.ceil
import kotlin.math.pow
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

internal val Int.centiseconds: Duration
    get() = (this * 10).milliseconds

internal val Duration.roundedUpCentiseconds: Int
    get() = ceil(inWholeMilliseconds / 10.0).toInt()

internal fun Int.pow(exponent: Int): Int =
    toDouble().pow(exponent).toInt()

internal fun Int.roundUpPowerOf2(): Int {
    var result = this - 1
    result = result or (result shr 1)
    result = result or (result shr 2)
    result = result or (result shr 4)
    result = result or (result shr 8)
    result = result or (result shr 16)
    return result + 1
}

internal fun Int.toHexByteString(): String {
    var hexString = toString(16).uppercase()
    if (hexString.length == 1) {
        hexString = "0$hexString"
    }
    return "0x$hexString"
}
