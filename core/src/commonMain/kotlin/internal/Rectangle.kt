package com.shakster.gifkt.internal

import com.shakster.gifkt.QuantizedImageData
import kotlin.math.max
import kotlin.math.min

internal data class Rectangle(
    val x: Int,
    val y: Int,
    val width: Int,
    val height: Int,
) {

    infix fun union(other: Rectangle): Rectangle {
        if (this == other) return this
        val x1 = min(x, other.x)
        val y1 = min(y, other.y)
        val x2 = max(x + width, other.x + other.width)
        val y2 = max(y + height, other.y + other.height)
        return Rectangle(x1, y1, x2 - x1, y2 - y1)
    }
}

internal val QuantizedImageData.bounds: Rectangle
    get() = Rectangle(x, y, width, height)
