package com.shakster.gifkt.internal

import com.shakster.gifkt.ColorDistanceCalculator

internal data class Image(
    val argb: IntArray,
    val width: Int,
    val height: Int,
) {

    fun cropOrPad(width: Int, height: Int): Image =
        if (this.width == width && this.height == height) {
            this
        } else if (this.width == width) {
            val newArgb = argb.copyOf(width * height)
            Image(newArgb, width, height)
        } else {
            val newArgb = IntArray(width * height) { i ->
                val x = i % width
                val y = i / width
                val index = x + y * this.width
                if (index < argb.size) {
                    argb[index]
                } else {
                    0
                }
            }
            Image(newArgb, width, height)
        }

    fun fillPartialAlpha(alphaFill: Int): Image {
        val newArgb = IntArray(argb.size) { i ->
            val pixel = argb[i]
            fillPartialAlpha(pixel, alphaFill)
        }
        return copy(argb = newArgb)
    }

    fun isSimilar(
        other: Image,
        tolerance: Double,
        colorDistanceCalculator: ColorDistanceCalculator,
    ): Boolean {
        return if (this === other) {
            true
        } else {
            val resizedOther = other.cropOrPad(width, height)
            val otherArgb = resizedOther.argb
            argb.forEachIndexed { i, pixel ->
                val otherPixel = otherArgb[i]
                val alpha = pixel ushr 24
                val otherAlpha = otherPixel ushr 24
                val similar = if (alpha == 0 && otherAlpha == 0) {
                    true
                } else if (alpha != otherAlpha) {
                    false
                } else if (tolerance == 0.0) {
                    pixel == otherPixel
                } else {
                    colorDistanceCalculator.colorDistance(pixel, otherPixel) <= tolerance
                }
                if (!similar) {
                    return false
                }
            }
            true
        }
    }

    fun fillTransparent(other: Image): Image {
        if (this === other) {
            return this
        }
        val fixedDimensions = other.cropOrPad(width, height)
        val filledRgb = IntArray(argb.size) { i ->
            val pixel = argb[i]
            val alpha = pixel ushr 24
            if (alpha == 0) {
                fixedDimensions.argb[i]
            } else {
                pixel
            }
        }
        return copy(argb = filledRgb)
    }

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

internal const val ALPHA_FILL_MASK: Int = 0xFF shl 24

private fun fillPartialAlpha(argb: Int, alphaFill: Int): Int {
    if (alphaFill < 0) {
        return argb
    }
    val alpha = argb ushr 24
    if (alpha == 0) {
        return 0
    }
    if (alpha == 0xFF) {
        return argb
    }

    val red = argb shr 16 and 0xFF
    val green = argb shr 8 and 0xFF
    val blue = argb and 0xFF

    val backgroundRed = alphaFill shr 16 and 0xFF
    val backgroundGreen = alphaFill shr 8 and 0xFF
    val backgroundBlue = alphaFill and 0xFF

    val newRed = compositeAlpha(alpha, red, backgroundRed)
    val newGreen = compositeAlpha(alpha, green, backgroundGreen)
    val newBlue = compositeAlpha(alpha, blue, backgroundBlue)

    return ALPHA_FILL_MASK or (newRed shl 16) or (newGreen shl 8) or newBlue
}

private fun compositeAlpha(alpha: Int, color: Int, backgroundColor: Int): Int {
    val opacity = alpha / 255.0
    return (color * opacity + backgroundColor * (1 - opacity)).toInt()
}
