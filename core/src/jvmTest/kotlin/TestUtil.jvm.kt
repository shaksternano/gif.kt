package com.shakster.gifkt

import java.awt.image.BufferedImage
import java.io.InputStream
import javax.imageio.ImageIO

private object TestUtil

fun getResource(path: String): InputStream =
    TestUtil::class.java.classLoader.getResourceAsStream(path)
        ?: throw IllegalArgumentException("Resource not found: $path")

fun loadImage(path: String): BufferedImage {
    val image = ImageIO.read(getResource(path))
    val argbArray = image.rgb
    val fixedAlpha = IntArray(argbArray.size) { i ->
        val pixel = argbArray[i]
        val rgb = RGB(pixel)
        if (rgb.alpha == 0) {
            0
        } else {
            pixel
        }
    }
    val fixedImage = BufferedImage(
        image.width,
        image.height,
        BufferedImage.TYPE_INT_ARGB,
    )
    fixedImage.rgb = fixedAlpha
    return fixedImage
}

fun readGifFrames(resourcePath: String): List<ImageFrame> {
    val bytes = getResource(resourcePath).use {
        it.readBytes()
    }
    return GifDecoder(bytes.asRandomAccess())
        .asSequence()
        .toList()
}
