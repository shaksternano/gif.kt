package com.shakster.gifkt

import com.shakster.gifkt.internal.equalsImpl
import com.shakster.gifkt.internal.hashCodeImpl
import com.shakster.gifkt.internal.toStringImpl
import kotlin.time.Duration
import kotlin.time.toJavaDuration
import kotlin.time.toKotlinDuration
import java.time.Duration as JavaDuration

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
 */
actual data class ImageFrame actual constructor(
    actual val argb: IntArray,
    actual val width: Int,
    actual val height: Int,
    actual val duration: Duration,
    actual val timestamp: Duration,
    actual val index: Int,
) : Comparable<ImageFrame> {

    /**
     * Constructs an [ImageFrame].
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
     */
    constructor(
        argb: IntArray,
        width: Int,
        height: Int,
        duration: JavaDuration,
        timestamp: JavaDuration,
        index: Int,
    ) : this(
        argb = argb,
        width = width,
        height = height,
        duration = duration.toKotlinDuration(),
        timestamp = timestamp.toKotlinDuration(),
        index = index,
    )

    /**
     * The duration of the frame.
     */
    inline val javaDuration: JavaDuration
        get() = duration.toJavaDuration()

    /**
     * The timestamp of the frame.
     */
    inline val javaTimestamp: JavaDuration
        get() = timestamp.toJavaDuration()

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
