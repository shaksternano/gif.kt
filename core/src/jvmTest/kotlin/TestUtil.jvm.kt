package com.shakster.gifkt

import java.awt.image.BufferedImage
import java.io.InputStream
import javax.imageio.ImageIO

private object TestUtil

fun getResource(path: String): InputStream =
    TestUtil::class.java.classLoader.getResourceAsStream(path)
        ?: throw IllegalArgumentException("Resource not found: $path")

fun loadImage(path: String): BufferedImage =
    ImageIO.read(getResource(path))

var BufferedImage.rgb: IntArray
    get() = getRGB(0, 0, width, height, null, 0, width)
    set(rgb) = setRGB(0, 0, width, height, rgb, 0, width)

fun readGifFrames(resourcePath: String): List<ImageFrame> {
    val bytes = getResource(resourcePath).use {
        it.readBytes()
    }
    return GifDecoder(bytes)
        .asSequence()
        .toList()
}
