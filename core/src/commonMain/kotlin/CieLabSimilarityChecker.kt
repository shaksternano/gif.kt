package com.shakster.gifkt

import kotlin.math.pow

data object CieLabSimilarityChecker : ColorSimilarityChecker {

    override fun isSimilar(rgb1: RGB, rgb2: RGB, tolerance: Double): Boolean {
        val (l1, a1, b1) = rgbToCieLab(rgb1.red, rgb1.green, rgb1.blue)
        val (l2, a2, b2) = rgbToCieLab(rgb2.red, rgb2.green, rgb2.blue)
        // Euclidean distance
        val lComponent = l1 - l2
        val aComponent = a1 - a2
        val bComponent = b1 - b2
        val distance = lComponent * lComponent + aComponent * aComponent + bComponent * bComponent
        return (distance / 10000).coerceAtMost(1.0) <= tolerance * tolerance
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
