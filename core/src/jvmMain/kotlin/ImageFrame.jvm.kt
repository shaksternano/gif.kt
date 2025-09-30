@file:JvmName("ImageFrameJvm")

package com.shakster.gifkt

import com.shakster.gifkt.internal.*
import java.awt.image.BufferedImage
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
 *
 * @throws IllegalArgumentException If [width] x [height] is not equal to [argb].[size][IntArray.size]or [duration] is negative.
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
     * @throws IllegalArgumentException If [width] x [height] is not equal to [argb].[size][IntArray.size] or [duration] is negative.
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
     * Constructs an [ImageFrame] from a [BufferedImage].
     *
     * @param image The [BufferedImage] containing the pixel data for the frame.
     *
     * @param duration The duration of the frame.
     *
     * @param timestamp The timestamp of the frame.
     *
     * @param index The index of the frame.
     *
     * @throws IllegalArgumentException If [duration] is negative.
     */
    constructor(
        image: BufferedImage,
        duration: Duration,
        timestamp: Duration,
        index: Int,
    ) : this(
        argb = image.argb,
        width = image.width,
        height = image.height,
        duration = duration,
        timestamp = timestamp,
        index = index,
    )

    /**
     * Constructs an [ImageFrame] from a [BufferedImage].
     *
     * @param image The [BufferedImage] containing the pixel data for the frame.
     *
     * @param duration The duration of the frame.
     *
     * @param timestamp The timestamp of the frame.
     *
     * @param index The index of the frame.
     *
     * @throws IllegalArgumentException If [duration] is negative.
     */
    constructor(
        image: BufferedImage,
        duration: JavaDuration,
        timestamp: JavaDuration,
        index: Int,
    ) : this(
        argb = image.argb,
        width = image.width,
        height = image.height,
        duration = duration.toKotlinDuration(),
        timestamp = timestamp.toKotlinDuration(),
        index = index,
    )

    init {
        checkDimensions(argb, width, height)
        checkDurationIsNonNegative(duration)
    }

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
     * Converts this frame to a [BufferedImage].
     *
     * @return A [BufferedImage] representation of this frame.
     */
    fun toBufferedImage(): BufferedImage {
        val image = BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)
        image.argb = argb
        return image
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

/**
 * The pixel data of this [BufferedImage].
 * Each element in the array represents a pixel in ARGB format,
 * going row by row from top to bottom.
 */
inline var BufferedImage.argb: IntArray
    get() = getRGB(0, 0, width, height, null, 0, width)
    set(argb) = setRGB(0, 0, width, height, argb, 0, width)
