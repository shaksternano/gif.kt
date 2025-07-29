package com.shakster.gifkt

import com.shakster.gifkt.internal.CieLabSimilarityChecker
import com.shakster.gifkt.internal.EuclideanSimilarityChecker
import kotlin.jvm.JvmField
import kotlin.jvm.JvmStatic

fun interface ColorSimilarityChecker {

    fun isSimilar(rgb1: RGB, rgb2: RGB, tolerance: Double): Boolean

    companion object {
        @JvmField
        val EUCLIDEAN: ColorSimilarityChecker = euclidean(1.0, 1.0, 1.0)

        @JvmField
        val EUCLIDEAN_LUMINANCE_WEIGHTING: ColorSimilarityChecker = euclidean(2.99, 5.87, 1.14)

        @JvmField
        val CIELAB: ColorSimilarityChecker = CieLabSimilarityChecker

        @JvmStatic
        fun euclidean(redWeight: Double, greenWeight: Double, blueWeight: Double): ColorSimilarityChecker {
            return EuclideanSimilarityChecker(redWeight, greenWeight, blueWeight)
        }
    }
}
