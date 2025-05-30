@file:JvmName("GifDecoderJvm")

package com.shakster.gifkt

import java.time.Duration
import kotlin.time.toJavaDuration

val GifDecoder.javaDuration: Duration
    get() = duration.toJavaDuration()
