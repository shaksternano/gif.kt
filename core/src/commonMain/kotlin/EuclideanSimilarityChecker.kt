package com.shakster.gifkt

data class EuclideanSimilarityChecker(
    private val redWeight: Double,
    private val greenWeight: Double,
    private val blueWeight: Double,
) : ColorSimilarityChecker {

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

    private val maxDistance: Double = 255 * 255 * (redWeight + greenWeight + blueWeight)

    override fun isSimilar(rgb1: RGB, rgb2: RGB, tolerance: Double): Boolean {
        val redComponent = rgb1.red - rgb2.red
        val greenComponent = rgb1.green - rgb2.green
        val blueComponent = rgb1.blue - rgb2.blue
        val distance = redComponent * redComponent * redWeight +
            greenComponent * greenComponent * greenWeight +
            blueComponent * blueComponent * blueWeight
        return distance / maxDistance <= tolerance * tolerance
    }
}
