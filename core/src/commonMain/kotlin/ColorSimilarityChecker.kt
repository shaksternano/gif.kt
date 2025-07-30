package com.shakster.gifkt

import com.shakster.gifkt.internal.CieLabSimilarityChecker
import com.shakster.gifkt.internal.EuclideanSimilarityChecker
import kotlin.jvm.JvmField
import kotlin.jvm.JvmStatic

/**
 * Interface for checking the similarity between two RGB colors.
 *
 * This interface defines a function to determine if two RGB colors are similar
 * within a specified tolerance.
 */
fun interface ColorSimilarityChecker {

    /**
     * Checks if two RGB colors are similar within a specified tolerance.
     *
     * @param rgb1 The first RGB color to compare.
     *
     * @param rgb2 The second RGB color to compare.
     *
     * @param tolerance A value between 0 and 1 representing the maximum allowed color difference.
     */
    fun isSimilar(rgb1: RGB, rgb2: RGB, tolerance: Double): Boolean

    companion object {
        /**
         * A [ColorSimilarityChecker] that uses the Euclidean distance
         * in the RGB color space to determine similarity.
         */
        @JvmField
        val EUCLIDEAN: ColorSimilarityChecker = euclidean(1.0, 1.0, 1.0)

        /**
         * A [ColorSimilarityChecker] that uses the Euclidean distance
         * in the RGB color space to determine similarity,
         * weighted by the perceived luminance of each color channel.
         */
        @JvmField
        val EUCLIDEAN_LUMINANCE_WEIGHTING: ColorSimilarityChecker = euclidean(2.99, 5.87, 1.14)

        /**
         * A [ColorSimilarityChecker] that uses the Euclidean distance
         * in the CIELAB color space to determine similarity.
         * This checker is more accurate to human perception,
         * but has slower performance.
         */
        @JvmField
        val CIELAB: ColorSimilarityChecker = CieLabSimilarityChecker

        /**
         * Creates a [ColorSimilarityChecker] that uses the Euclidean distance
         * in the RGB color space to determine similarity.
         *
         * @param redWeight The weight for the red channel.
         *
         * @param greenWeight The weight for the green channel.
         *
         * @param blueWeight The weight for the blue channel.
         *
         * @return A [ColorSimilarityChecker] that uses the specified weights.
         */
        @JvmStatic
        fun euclidean(redWeight: Double, greenWeight: Double, blueWeight: Double): ColorSimilarityChecker {
            return EuclideanSimilarityChecker(redWeight, greenWeight, blueWeight)
        }
    }
}
