package io.github.shaksternano.gifcodec

import java.awt.image.BufferedImage
import java.awt.image.ColorConvertOp
import java.io.InputStream
import javax.imageio.ImageIO
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TestNeuQuant {

    @Test
    fun test512Colors() {
        testMaxColors(512)
    }

    @Test
    fun test256Colors() {
        testMaxColors(256)
    }

    @Test
    fun test255Colors() {
        testMaxColors(255)
    }

    @Test
    fun test200Colors() {
        testMaxColors(200)
    }

    @Test
    fun test128Colors() {
        testMaxColors(128)
    }

    private fun testMaxColors(maxColors: Int) {
        val pixels = loadRgbPixels("media/sonic.png")
        val neuQuant = NeuQuant(
            image = pixels,
            maxColors = maxColors,
            samplingFactor = 10,
        )
        val colorTable = neuQuant.process()
        assertTrue(colorTable.size % 3 == 0)
        assertEquals(colorTable.size, maxColors * 3)
        assertTrue(colorTable.all { it.toUByte() in 0u..255u })
    }

    @Test
    fun testQuantization() {
        val pixels = loadRgbPixels("media/sonic.png")
        val neuQuant = NeuQuant(
            image = pixels,
            samplingFactor = 10,
        )
        val colorTable = neuQuant.process()
        val quantizedPixels = pixels.asList()
            .chunked(3)
            .flatMap { (red, green, blue) ->
                val index = neuQuant.map(
                    red.toInt() and 0xFF,
                    green.toInt() and 0xFF,
                    blue.toInt() and 0xFF,
                )
                val quantizedRed = colorTable[index * 3]
                val quantizedGreen = colorTable[index * 3 + 1]
                val quantizedBlue = colorTable[index * 3 + 2]
                listOf(quantizedRed, quantizedGreen, quantizedBlue)
            }
        val expectedPixels = loadRgbPixels("media/sonic-quantized.png").asList()
        assertEquals(expectedPixels, quantizedPixels)
    }
}

fun loadRgbPixels(path: String): ByteArray {
    val image = loadImage(path)
    val rgb = ByteArray(image.width * image.height * 3)
    for (y in 0 until image.height) {
        for (x in 0 until image.width) {
            val pixel = image.getRGB(x, y)
            val index = (y * image.width + x) * 3
            rgb[index] = pixel.toByte()
            rgb[index + 1] = (pixel shr 8).toByte()
            rgb[index + 2] = (pixel shr 16).toByte()
        }
    }
    return rgb
}

private fun loadImage(path: String): BufferedImage =
    ImageIO.read(getResource(path))

private fun getResource(path: String): InputStream =
    TestNeuQuant::class.java.classLoader.getResourceAsStream(path)
        ?: throw IllegalArgumentException("Resource not found: $path")

private fun BufferedImage.convertType(type: Int): BufferedImage =
    if (this.type == type) this
    else {
        val newType = BufferedImage(width, height, type)
        val convertOp = ColorConvertOp(null)
        convertOp.filter(this, newType)
    }
