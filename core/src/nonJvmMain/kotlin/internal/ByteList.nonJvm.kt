package com.shakster.gifkt.internal

internal actual fun ByteArray.contentEquals(other: ByteArray, size: Int): Boolean {
    return if (this === other) true
    else contentEqualsCommon(other, size)
}
