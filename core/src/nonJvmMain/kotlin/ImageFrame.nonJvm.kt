package com.shakster.gifkt

import com.shakster.gifkt.internal.equalsImpl
import com.shakster.gifkt.internal.hashCodeImpl
import com.shakster.gifkt.internal.toStringImpl
import kotlin.time.Duration

actual data class ImageFrame actual constructor(
    actual val argb: IntArray,
    actual val width: Int,
    actual val height: Int,
    actual val duration: Duration,
    actual val timestamp: Duration,
    actual val index: Int,
) {
    override fun equals(other: Any?): Boolean = equalsImpl(other)

    override fun hashCode(): Int = hashCodeImpl()

    override fun toString(): String = toStringImpl()
}
