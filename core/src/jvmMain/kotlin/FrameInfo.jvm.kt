@file:JvmName("FrameInfoJvm")

package com.shakster.gifkt

import java.time.Duration
import kotlin.time.toJavaDuration

val FrameInfo.javaDuration: Duration
    get() = duration.toJavaDuration()

val FrameInfo.javaTimestamp: Duration
    get() = timestamp.toJavaDuration()
