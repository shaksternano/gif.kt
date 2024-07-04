package io.github.shaksternano.gifcodec

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
        ) {
            var hexString = it.toString(16).uppercase()
            if (hexString.length == 1) {
                hexString = "0$hexString"
            }
            "0x$hexString"
        }
}

fun List<Int>.asHexByteList(): HexByteList =
    HexByteList(this)
