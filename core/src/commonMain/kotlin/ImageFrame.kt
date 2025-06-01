package com.shakster.gifkt

import kotlin.time.Duration

expect class ImageFrame(
    argb: IntArray,
    width: Int,
    height: Int,
    duration: Duration,
    timestamp: Duration,
    index: Int,
) {
    val argb: IntArray
    val width: Int
    val height: Int
    val duration: Duration
    val timestamp: Duration
    val index: Int

    operator fun component1(): IntArray
    operator fun component2(): Int
    operator fun component3(): Int
    operator fun component4(): Duration
    operator fun component5(): Duration
    operator fun component6(): Int
}

/*
 * This can't be an expect class method because the
 * implementing data class's `copy` method has default
 * arguments, which is not allowed in actual methods.
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
