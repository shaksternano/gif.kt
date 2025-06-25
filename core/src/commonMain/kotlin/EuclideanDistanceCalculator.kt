package com.shakster.gifkt

import kotlin.jvm.JvmField
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

    init {
        require(redWeight >= 0) {
            "Red weight must be non-negative"
        }
        require(greenWeight >= 0) {
            "Green weight must be non-negative"
        }
        require(blueWeight >= 0) {
            "Blue weight must be non-negative"
        }
        require(redWeight > 0 || greenWeight > 0 || blueWeight > 0) {
            "At least one weight must be positive"
        }
    }

    private val maxDistance: Double = sqrt(255 * 255 * (redWeight + greenWeight + blueWeight))

    override fun colorDistance(rgb1: RGB, rgb2: RGB): Double {
        val redComponent = rgb1.red - rgb2.red
        val greenComponent = rgb1.green - rgb2.green
        val blueComponent = rgb1.blue - rgb2.blue
        val distance = sqrt(
            redComponent * redComponent * redWeight +
                greenComponent * greenComponent * greenWeight +
                blueComponent * blueComponent * blueWeight
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
