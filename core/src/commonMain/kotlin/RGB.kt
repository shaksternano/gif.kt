package com.shakster.gifkt

import kotlin.jvm.JvmInline

/**
 * Represents an ARGB color, with one byte per component.
 *
 * @property value An ARGB color value packed into an [Int].
 */
@JvmInline
value class RGB(val value: Int) {

    /**
     * Creates an `RGB` instance from red, green, blue, and alpha components.
     * Each value must be in the range 0 to 255 inclusive.
     * If alpha is not provided, it defaults to 255, making the color opaque.
     *
     * @param red The red component of the color.
     * @param green The green component of the color.
     * @param blue The blue component of the color.
     * @param alpha The alpha component of the color. Defaults to 255 (opaque).
     */
    constructor(
        red: Int,
        green: Int,
        blue: Int,
        alpha: Int = 0xFF,
    ) : this(
        (alpha shl 24) or
            ((red and 0xFF) shl 16) or
            ((green and 0xFF) shl 8) or
            (blue and 0xFF)
    )

    /**
     * The red component of this color.
     */
    inline val red: Int
        get() = (value shr 16) and 0xFF

    /**
     * The green component of this color.
     */
    inline val green: Int
        get() = (value shr 8) and 0xFF

    /**
     * The blue component of this color.
     */
    inline val blue: Int
        get() = value and 0xFF

    /**
     * The alpha component of this color.
     */
    inline val alpha: Int
        get() = value ushr 24

    override fun toString(): String {
        return "RGB(red=$red, green=$green, blue=$blue, alpha=$alpha)"
    }
}
