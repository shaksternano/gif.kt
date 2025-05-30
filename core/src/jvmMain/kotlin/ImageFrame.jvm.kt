@file:JvmName("ImageFrameJvm")

package com.shakster.gifkt

import java.awt.image.BufferedImage
import java.time.Duration
import kotlin.time.toJavaDuration

val ImageFrame.javaDuration: Duration
    get() = duration.toJavaDuration()

val ImageFrame.javaTimestamp: Duration
    get() = timestamp.toJavaDuration()

var BufferedImage.rgb: IntArray
    get() = getRGB(0, 0, width, height, null, 0, width)
    set(rgb) = setRGB(0, 0, width, height, rgb, 0, width)

fun ImageFrame.toBufferedImage(): BufferedImage {
    val image = BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)
    image.rgb = argb
    return image
}
