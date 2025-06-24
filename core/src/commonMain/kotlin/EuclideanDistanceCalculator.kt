package com.shakster.gifkt

import kotlin.jvm.JvmField
import kotlin.math.pow
import kotlin.math.sqrt

data class EuclideanDistanceCalculator(
    private val redWeight: Double,
    private val greenWeight: Double,
    private val blueWeight: Double,
) : ColorDistanceCalculator {

    constructor(
        redWeight: Int,
        greenWeight: Int,
        blueWeight: Int,
    ) : this(
        redWeight.toDouble(),
        greenWeight.toDouble(),
        blueWeight.toDouble(),
    )

    private val maxDistance: Double = sqrt(255 * 255 * (redWeight + greenWeight + blueWeight))

    override fun colorDistance(rgb1: RGB, rgb2: RGB): Double {
        val distance = sqrt(
            (rgb1.red - rgb2.red).toDouble().pow(2) * redWeight +
                (rgb1.green - rgb2.green).toDouble().pow(2) * greenWeight +
                (rgb1.blue - rgb2.blue).toDouble().pow(2) * blueWeight
        )
        return distance / maxDistance
    }

    companion object {
        @JvmField
        val EQUAL_WEIGHTING: ColorDistanceCalculator = EuclideanDistanceCalculator(1, 1, 1)

        @JvmField
        val LUMINANCE_WEIGHTING: ColorDistanceCalculator = EuclideanDistanceCalculator(2.99, 5.87, 1.14)
    }
}
