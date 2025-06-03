package com.shakster.gifkt

import kotlin.time.Duration

actual data class FrameInfo actual constructor(
    actual val duration: Duration,
    actual val timestamp: Duration,
) : Comparable<FrameInfo> {

    actual override fun compareTo(other: FrameInfo): Int {
        return timestamp.compareTo(other.timestamp)
    }
}
