package com.shakster.gifkt

import kotlin.time.Duration

expect class FrameInfo(
    duration: Duration,
    timestamp: Duration,
) : Comparable<FrameInfo> {

    val duration: Duration
    val timestamp: Duration

    override fun compareTo(other: FrameInfo): Int

    operator fun component1(): Duration
    operator fun component2(): Duration
}

/*
 * This can't be an expect class method because the
 * implementing data class's `copy` method has default
 * arguments, which is not allowed in actual methods.
 */
@Suppress("EXTENSION_SHADOWED_BY_MEMBER")
fun FrameInfo.copy(
    duration: Duration = this.duration,
    timestamp: Duration = this.timestamp,
): FrameInfo {
    return FrameInfo(duration, timestamp)
}
