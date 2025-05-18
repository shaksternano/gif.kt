package io.github.shaksternano.gifcodec.internal

internal data class QuantizedImageData(
    val imageColorIndices: ByteArray,
    val width: Int,
    val height: Int,
    val x: Int,
    val y: Int,
    val colorTable: ByteArray,
    val transparentColorIndex: Int,
) {

    val bounds: Rectangle
        get() = Rectangle(x, y, width, height)

    fun toImage(): Image {
        val argb = IntArray(imageColorIndices.size) { i ->
            val index = imageColorIndices[i].toInt() and 0xFF
            if (index == transparentColorIndex) {
                0
            } else {
                val offset = index * 3
                val red = colorTable[offset].toInt() and 0xFF
                val green = colorTable[offset + 1].toInt() and 0xFF
                val blue = colorTable[offset + 2].toInt() and 0xFF
                ALPHA_FILL_MASK or (red shl 16) or (green shl 8) or blue
            }
        }
        return Image(argb, width, height)
    }

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
