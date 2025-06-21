package com.shakster.gifkt

data class Image(
    val argb: IntArray,
    val width: Int,
    val height: Int,
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
