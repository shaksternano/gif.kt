package com.shakster.gifkt

import kotlin.jvm.JvmField

fun interface ColorDistanceCalculator {

    fun colorDistance(rgb1: RGB, rgb2: RGB): Double

    companion object {
        @JvmField
        val DEFAULT: ColorDistanceCalculator = EuclideanDistanceCalculator.LUMINANCE_WEIGHTING
    }
}
