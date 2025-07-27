package com.shakster.gifkt

import com.shakster.gifkt.internal.CieLabSimilarityChecker
import kotlin.jvm.JvmField

fun interface ColorSimilarityChecker {

    fun isSimilar(rgb1: RGB, rgb2: RGB, tolerance: Double): Boolean

    companion object {
        @JvmField
        val EUCLIDEAN: ColorSimilarityChecker = EuclideanSimilarityChecker(1.0, 1.0, 1.0)

        @JvmField
        val EUCLIDEAN_LUMINANCE_WEIGHTING: ColorSimilarityChecker = EuclideanSimilarityChecker(2.99, 5.87, 1.14)

        @JvmField
        val CIE_LAB: ColorSimilarityChecker = CieLabSimilarityChecker
    }
}
