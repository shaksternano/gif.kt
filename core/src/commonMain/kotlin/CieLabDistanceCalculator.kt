package com.shakster.gifkt

import kotlin.math.pow
import kotlin.math.sqrt

data object CieLabDistanceCalculator : ColorDistanceCalculator {

    override fun colorDistance(rgb1: Int, rgb2: Int): Double {
        if (rgb1 == rgb2) {
            return 0.0
        }

        val red1 = rgb1 shr 16 and 0xFF
        val green1 = rgb1 shr 8 and 0xFF
        val blue1 = rgb1 and 0xFF

        val red2 = rgb2 shr 16 and 0xFF
        val green2 = rgb2 shr 8 and 0xFF
        val blue2 = rgb2 and 0xFF

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
}
