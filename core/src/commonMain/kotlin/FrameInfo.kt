package com.shakster.gifkt

import kotlin.time.Duration

/**
 * Information about a single frame.
 *
 * @param duration The duration of the frame.
 *
 * @param timestamp The timestamp of the frame.
 */
expect class FrameInfo(
    duration: Duration,
    timestamp: Duration,
) : Comparable<FrameInfo> {

    /**
     * The duration of the frame.
     */
    val duration: Duration

    /**
     * The timestamp of the frame.
     */
    val timestamp: Duration

    /**
     * Compares this frame to another frame based on their [timestamp]s.
     */
    override fun compareTo(other: FrameInfo): Int

    /**
     * The duration of the frame.
     */
    operator fun component1(): Duration

    /**
     * The timestamp of the frame.
     */
    operator fun component2(): Duration
}

/*
 * This can't be an expect class method because the
 * implementing data class's `copy` method has default
 * arguments, which is not allowed in actual methods.
 */
/**
 * Creates a copy of this [FrameInfo] with the specified properties.
 *
 * @param duration The duration of the frame.
 *
 * @param timestamp The timestamp of the frame.
 */
@Suppress("EXTENSION_SHADOWED_BY_MEMBER")
fun FrameInfo.copy(
    duration: Duration = this.duration,
    timestamp: Duration = this.timestamp,
): FrameInfo {
    return FrameInfo(duration, timestamp)
}
