package com.shakster.gifkt

/**
 * Represents an image with pixel data in ARGB format.
 *
 * @property argb The pixel data in ARGB format.
 * @property width The width of the image in pixels.
 * @property height The height of the image in pixels.
 * @property empty Indicates whether the image is empty,
 * used to avoid iteration over [argb]
 */
data class Image(
    val argb: IntArray,
    val width: Int,
    val height: Int,
    val empty: Boolean = false,
) {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as Image

        if (width != other.width) return false
        if (height != other.height) return false
        if (!argb.contentEquals(other.argb)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = argb.contentHashCode()
        result = 31 * result + width
        result = 31 * result + height
        return result
    }

    override fun toString(): String {
        val argbHash = argb.contentHashCode().toString(16)
        val argbString = "IntArray(size=${argb.size}, hashCode=$argbHash)"
        return "Image(" +
            "argb=$argbString" +
            ", width=$width" +
            ", height=$height" +
            ")"
    }
}
