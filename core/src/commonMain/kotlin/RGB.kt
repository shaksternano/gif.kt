package com.shakster.gifkt

import kotlin.jvm.JvmInline

@JvmInline
value class RGB(val value: Int) {

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
