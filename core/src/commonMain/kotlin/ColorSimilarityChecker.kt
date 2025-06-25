package com.shakster.gifkt

import kotlin.jvm.JvmField

fun interface ColorSimilarityChecker {

    fun isSimilar(rgb1: RGB, rgb2: RGB, tolerance: Double): Boolean

    companion object {
        @JvmField
        val DEFAULT: ColorSimilarityChecker = EuclideanSimilarityChecker.LUMINANCE_WEIGHTING
    }
}
