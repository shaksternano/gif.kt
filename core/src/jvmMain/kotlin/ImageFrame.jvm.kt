@file:JvmName("ImageFrameJvm")

package com.shakster.gifkt

import com.shakster.gifkt.internal.equalsImpl
import com.shakster.gifkt.internal.hashCodeImpl
import com.shakster.gifkt.internal.toStringImpl
import java.awt.image.BufferedImage
import kotlin.time.Duration
import kotlin.time.toJavaDuration
import kotlin.time.toKotlinDuration
import java.time.Duration as JavaDuration

actual data class ImageFrame actual constructor(
    actual val argb: IntArray,
    actual val width: Int,
    actual val height: Int,
    actual val duration: Duration,
    actual val timestamp: Duration,
    actual val index: Int,
) : Comparable<ImageFrame> {

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

    constructor(
        image: BufferedImage,
        duration: Duration,
        timestamp: Duration,
        index: Int,
    ) : this(
        argb = image.rgb,
        width = image.width,
        height = image.height,
        duration = duration,
        timestamp = timestamp,
        index = index,
    )

    constructor(
        image: BufferedImage,
        duration: JavaDuration,
        timestamp: JavaDuration,
        index: Int,
    ) : this(
        argb = image.rgb,
        width = image.width,
        height = image.height,
        duration = duration.toKotlinDuration(),
        timestamp = timestamp.toKotlinDuration(),
        index = index,
    )

    inline val javaDuration: JavaDuration
        get() = duration.toJavaDuration()
    inline val javaTimestamp: JavaDuration
        get() = timestamp.toJavaDuration()

    fun toBufferedImage(): BufferedImage {
        val image = BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)
        image.rgb = argb
        return image
    }

    actual override fun compareTo(other: ImageFrame): Int {
        return index.compareTo(other.index)
    }

    override fun equals(other: Any?): Boolean = equalsImpl(other)

    override fun hashCode(): Int = hashCodeImpl()

    override fun toString(): String = toStringImpl()
}

inline var BufferedImage.rgb: IntArray
    get() = getRGB(0, 0, width, height, null, 0, width)
    set(rgb) = setRGB(0, 0, width, height, rgb, 0, width)
