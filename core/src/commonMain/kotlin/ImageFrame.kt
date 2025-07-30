package com.shakster.gifkt

import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

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
expect class ImageFrame(
    argb: IntArray,
    width: Int,
    height: Int,
    duration: Duration,
    timestamp: Duration,
    index: Int,
) : Comparable<ImageFrame> {

    /**
     * The ARGB pixel data for the frame.
     * Each element in the array represents a pixel in ARGB format,
     * going row by row from top to bottom.
     */
    val argb: IntArray

    /**
     * The width of the frame in pixels.
     */
    val width: Int

    /**
     * The height of the frame in pixels.
     */
    val height: Int

    /**
     * The duration of the frame.
     */
    val duration: Duration

    /**
     * The timestamp of the frame.
     */
    val timestamp: Duration

    /**
     * The index of the frame.
     */
    val index: Int

    /**
     * Compares this frame to another frame based on their [indices][index].
     */
    override fun compareTo(other: ImageFrame): Int

    /**
     * The ARGB pixel data for the frame.
     * Each element in the array represents a pixel in ARGB format,
     * going row by row from top to bottom.
     */
    operator fun component1(): IntArray

    /**
     * The width of the frame in pixels.
     */
    operator fun component2(): Int

    /**
     * The height of the frame in pixels.
     */
    operator fun component3(): Int

    /**
     * The duration of the frame.
     */
    operator fun component4(): Duration

    /**
     * The timestamp of the frame.
     */
    operator fun component5(): Duration

    /**
     * The index of the frame.
     */
    operator fun component6(): Int
}

/*
 * This can't be an expect class method because the
 * implementing data class's `copy` method has default
 * arguments, which is not allowed in actual methods.
 */
/**
 * Creates a copy of this [ImageFrame] with the specified properties.
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
@Suppress("EXTENSION_SHADOWED_BY_MEMBER")
fun ImageFrame.copy(
    argb: IntArray = this.argb,
    width: Int = this.width,
    height: Int = this.height,
    duration: Duration = this.duration,
    timestamp: Duration = this.timestamp,
    index: Int = this.index,
): ImageFrame {
    return ImageFrame(argb, width, height, duration, timestamp, index)
}

/**
 * Returns a [Duration] equal to this [Int] number of centiseconds.
 */
inline val Int.centiseconds: Duration
    get() = (this * 10).milliseconds
