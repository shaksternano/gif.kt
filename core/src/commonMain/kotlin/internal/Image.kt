package com.shakster.gifkt.internal

import kotlin.math.pow
import kotlin.math.sqrt

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

    fun isSimilar(other: Image, tolerance: Double): Boolean =
        if (this === other) {
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
                    colorDistance(pixel, otherPixel) <= tolerance
                }
                if (!similar) {
                    return false
                }
            }
            true
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

fun colorDistance(color1: Int, color2: Int): Double {
    if (color1 == color2) {
        return 0.0
    }

    val red1 = color1 shr 16 and 0xFF
    val green1 = color1 shr 8 and 0xFF
    val blue1 = color1 and 0xFF

    val red2 = color2 shr 16 and 0xFF
    val green2 = color2 shr 8 and 0xFF
    val blue2 = color2 and 0xFF

    val (l1, a1, b1) = rgbToCieLab(red1, green1, blue1)
    val (l2, a2, b2) = rgbToCieLab(red2, green2, blue2)

    // Euclidean distance
    val distance = sqrt((l1 - l2).pow(2) + (a1 - a2).pow(2) + (b1 - b2).pow(2))
    return (distance / 100).coerceIn(0.0, 1.0)
}

private fun rgbToCieLab(red: Int, green: Int, blue: Int): Triple<Double, Double, Double> {
    val (x, y, z) = rgbToXyz(red, green, blue)
    return xyzToCieLab(x, y, z)
}

/*
 * Reference:
 * https://www.easyrgb.com/en/math.php
 */
private fun rgbToXyz(red: Int, green: Int, blue: Int): Triple<Double, Double, Double> {
    var red1 = red / 255.0
    var green1 = green / 255.0
    var blue1 = blue / 255.0

    red1 = if (red1 > 0.04045) {
        ((red1 + 0.055) / 1.055).pow(2.4)
    } else {
        red1 / 12.92
    }
    green1 = if (green1 > 0.04045) {
        ((green1 + 0.055) / 1.055).pow(2.4)
    } else {
        green1 / 12.92
    }
    blue1 = if (blue1 > 0.04045) {
        ((blue1 + 0.055) / 1.055).pow(2.4)
    } else {
        blue1 / 12.92
    }

    red1 *= 100
    green1 *= 100
    blue1 *= 100

    val x = red1 * 0.4124 + green1 * 0.3576 + blue1 * 0.1805
    val y = red1 * 0.2126 + green1 * 0.7152 + blue1 * 0.0722
    val z = red1 * 0.0193 + green1 * 0.1192 + blue1 * 0.9505
    return Triple(x, y, z)
}

/*
 * Reference:
 * https://www.easyrgb.com/en/math.php
 */
private fun xyzToCieLab(x: Double, y: Double, z: Double): Triple<Double, Double, Double> {
    // 2° (CIE 1931), D65 (Daylight, sRGB, Adobe-RGB)
    var x1 = x / 95.047
    var y1 = y / 100.000
    var z1 = z / 108.883

    x1 = if (x1 > 0.008856) {
        x1.pow(1.0 / 3)
    } else {
        7.787 * x1 + 16.0 / 116
    }
    y1 = if (y1 > 0.008856) {
        y1.pow(1.0 / 3)
    } else {
        7.787 * y1 + 16.0 / 116
    }
    z1 = if (z1 > 0.008856) {
        z1.pow(1.0 / 3)
    } else {
        7.787 * z1 + 16.0 / 116
    }

    val l = 116 * y1 - 16
    val a = 500 * (x1 - y1)
    val b = 200 * (y1 - z1)
    return Triple(l, a, b)
}
