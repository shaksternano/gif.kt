package com.shakster.gifkt

fun interface ColorDistanceCalculator {

    fun colorDistance(rgb1: RGB, rgb2: RGB): Double
}
