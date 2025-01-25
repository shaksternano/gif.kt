package io.github.shaksternano.gifcodec

import kotlinx.io.Buffer
import kotlinx.io.readByteArray
import java.awt.image.BufferedImage
import java.io.InputStream
import javax.imageio.ImageIO

fun Buffer.readByteList(): HexByteList =
    readByteArray().map { it.toInt() and 0xFF }.asHexByteList()

fun getResource(path: String): InputStream =
    TestNeuQuant::class.java.classLoader.getResourceAsStream(path)
        ?: throw IllegalArgumentException("Resource not found: $path")

fun loadImage(path: String): BufferedImage =
    ImageIO.read(getResource(path))
