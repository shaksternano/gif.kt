package com.shakster.gifkt

fun interface ColorDistanceCalculator {

    fun colorDistance(rgb1: Int, rgb2: Int): Double
}
