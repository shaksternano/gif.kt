package com.shakster.gifkt

/**
 * Represents quantized image data used in GIF encoding.
 *
 * @param imageColorIndices The indices of the colors in the image.
 * Each index corresponds to a color in the [colorTable].
 *
 * @param width The width of the image in pixels.
 *
 * @param height The height of the image in pixels.
 *
 * @param x The x-coordinate of the top-left corner of the image within the GIF canvas.
 *
 * @param y The y-coordinate of the top-left corner of the image within the GIF canvas.
 *
 * @param colorTable The color table of the image,
 * where each color is represented by three consecutive bytes: red, green, and blue.
 *
 * @param transparentColorIndex The index of the transparent color.
 * A value of -1 indicates that there is no transparency in the image.
 */
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
