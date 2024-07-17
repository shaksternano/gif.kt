package io.github.shaksternano.gifcodec

import java.awt.image.BufferedImage
import java.awt.image.ColorConvertOp
import java.awt.image.DataBufferByte
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
        val pixels = loadBgrPixels("media/sonic.png")
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
        val pixels = loadBgrPixels("media/sonic.png")
        val neuQuant = NeuQuant(
            image = pixels,
            samplingFactor = 10,
        )
        val colorTable = neuQuant.process()
        val quantizedPixels = pixels.asList()
            .chunked(3)
            .flatMap { (blue, green, red) ->
                val index = neuQuant.map(
                    blue.toInt() and 0xFF,
                    green.toInt() and 0xFF,
                    red.toInt() and 0xFF,
                )
                val quantizedRed = colorTable[index * 3]
                val quantizedGreen = colorTable[index * 3 + 1]
                val quantizedBlue = colorTable[index * 3 + 2]
                listOf(quantizedBlue, quantizedGreen, quantizedRed)
            }
        val expectedPixels = loadBgrPixels("media/sonic-quantized.png").asList()
        assertEquals(expectedPixels, quantizedPixels)
    }
}

fun loadBgrPixels(path: String): ByteArray {
    val image = loadImage(path).convertType(BufferedImage.TYPE_3BYTE_BGR)
    return (image.raster.dataBuffer as DataBufferByte).data
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
