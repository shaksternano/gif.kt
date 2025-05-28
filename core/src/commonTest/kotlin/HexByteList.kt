package com.shakster.gifkt

import com.shakster.gifkt.internal.toHexByteString
import kotlin.jvm.JvmInline

/**
 * For making byte lists more readable when tests fail.
 */
@JvmInline
value class HexByteList(
    private val bytes: List<Int>
) : List<Int> by bytes {

    constructor(vararg elements: Int) : this(elements.asList())

    override fun toString(): String =
        bytes.joinToString(
            prefix = "[",
            postfix = "]",
            transform = Int::toHexByteString,
        )
}

fun List<Int>.asHexByteList(): HexByteList =
    HexByteList(this)
