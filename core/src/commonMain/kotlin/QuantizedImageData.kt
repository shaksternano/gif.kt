package com.shakster.gifkt

data class QuantizedImageData(
    val imageColorIndices: ByteArray,
    val width: Int,
    val height: Int,
    val x: Int,
    val y: Int,
    val colorTable: ByteArray,
    val transparentColorIndex: Int,
) {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as QuantizedImageData

        if (width != other.width) return false
        if (height != other.height) return false
        if (x != other.x) return false
        if (y != other.y) return false
        if (transparentColorIndex != other.transparentColorIndex) return false
        if (!imageColorIndices.contentEquals(other.imageColorIndices)) return false
        if (!colorTable.contentEquals(other.colorTable)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = imageColorIndices.contentHashCode()
        result = 31 * result + width
        result = 31 * result + height
        result = 31 * result + x
        result = 31 * result + y
        result = 31 * result + colorTable.contentHashCode()
        result = 31 * result + transparentColorIndex
        return result
    }

    override fun toString(): String {
        val indicesHash = imageColorIndices.contentHashCode().toString(16)
        val indicesString = "ByteArray(size=${imageColorIndices.size}, hashCode=$indicesHash)"
        val colorTableHash = colorTable.contentHashCode().toString(16)
        val colorTableString = "ByteArray(size=${colorTable.size}, hashCode=$colorTableHash)"
        return "QuantizedImageData(" +
            "imageColorIndices=$indicesString" +
            ", width=$width" +
            ", height=$height" +
            ", x=$x" +
            ", y=$y" +
            ", colorTable=$colorTableString" +
            ", transparentColorIndex=$transparentColorIndex" +
            ")"
    }
}
