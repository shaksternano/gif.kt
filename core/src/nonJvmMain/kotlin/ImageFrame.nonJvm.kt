package com.shakster.gifkt

import com.shakster.gifkt.internal.*
import kotlin.time.Duration

/**
 * Stores a single frame's data.
 *
 * @param argb The ARGB pixel data for the frame.
 * Each element in the array represents a pixel in ARGB format,
 * going row by row from top to bottom.
 *
 * @param width The width of the frame in pixels.
 *
 * @param height The height of the frame in pixels.
 *
 * @param duration The duration of the frame.
 *
 * @param timestamp The timestamp of the frame.
 *
 * @param index The index of the frame.
 *
 * @throws IllegalArgumentException If [width] x [height] is not equal to [argb].[size][IntArray.size]
 * or [duration] is negative.
 */
actual data class ImageFrame actual constructor(
    actual val argb: IntArray,
    actual val width: Int,
    actual val height: Int,
    actual val duration: Duration,
    actual val timestamp: Duration,
    actual val index: Int,
) : Comparable<ImageFrame> {

    init {
        checkDimensions(argb, width, height)
        checkDurationIsNonNegative(duration)
    }

    /**
     * Compares this frame to another frame based on their [indices][index].
     */
    actual override fun compareTo(other: ImageFrame): Int {
        return index.compareTo(other.index)
    }

    override fun equals(other: Any?): Boolean = equalsImpl(other)

    override fun hashCode(): Int = hashCodeImpl()

    override fun toString(): String = toStringImpl()
}
