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

fun readGifFrames(resourcePath: String): List<ImageFrame> {
    val bytes = getResource(resourcePath).use {
        it.readBytes()
    }
    return GifDecoder(bytes.asRandomAccess())
        .asSequence()
        .toList()
}
