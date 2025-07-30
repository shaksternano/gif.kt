package com.shakster.gifkt

import kotlin.time.Duration

/**
 * Information about a single frame.
 *
 * @param duration The duration of the frame.
 *
 * @param timestamp The timestamp of the frame.
 */
actual data class FrameInfo actual constructor(
    actual val duration: Duration,
    actual val timestamp: Duration,
) : Comparable<FrameInfo> {

    /**
     * Compares this frame to another frame based on the [timestamp].
     */
    actual override fun compareTo(other: FrameInfo): Int {
        return timestamp.compareTo(other.timestamp)
    }
}
