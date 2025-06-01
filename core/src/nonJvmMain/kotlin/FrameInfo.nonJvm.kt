package com.shakster.gifkt

import kotlin.time.Duration

actual data class FrameInfo actual constructor(
    actual val duration: Duration,
    actual val timestamp: Duration,
)
