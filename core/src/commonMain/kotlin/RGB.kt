package com.shakster.gifkt

import kotlin.jvm.JvmInline

@JvmInline
value class RGB(val value: Int) {

    constructor(
        red: Int,
        green: Int,
        blue: Int,
        alpha: Int = 0xFF,
    ) : this(
        alpha shl 24 or
            (red and 0xFF) shl 16 or
            (green and 0xFF) shl 8 or
            (blue and 0xFF)
    )

    inline val red: Int
        get() = (value shr 16) and 0xFF

    inline val green: Int
        get() = (value shr 8) and 0xFF

    inline val blue: Int
        get() = value and 0xFF

    inline val alpha: Int
        get() = value ushr 24

    override fun toString(): String {
        return "RGB(red=$red, green=$green, blue=$blue, alpha=$alpha)"
    }
}
