package com.shakster.gifkt

import kotlin.time.Duration
import kotlin.time.toJavaDuration
import java.time.Duration as JavaDuration

actual data class FrameInfo actual constructor(
    actual val duration: Duration,
    actual val timestamp: Duration,
) : Comparable<FrameInfo> {

    val javaDuration: JavaDuration
        get() = duration.toJavaDuration()
    val javaTimestamp: JavaDuration
        get() = timestamp.toJavaDuration()

    actual override fun compareTo(other: FrameInfo): Int {
        return timestamp.compareTo(other.timestamp)
    }
}
