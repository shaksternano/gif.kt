package com.shakster.gifkt

import com.shakster.gifkt.internal.equalsImpl
import com.shakster.gifkt.internal.hashCodeImpl
import com.shakster.gifkt.internal.toStringImpl
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

    val javaDuration: JavaDuration
        get() = duration.toJavaDuration()
    val javaTimestamp: JavaDuration
        get() = timestamp.toJavaDuration()

    actual override fun compareTo(other: ImageFrame): Int {
        return index.compareTo(other.index)
    }

    override fun equals(other: Any?): Boolean = equalsImpl(other)

    override fun hashCode(): Int = hashCodeImpl()

    override fun toString(): String = toStringImpl()
}
