@file:JvmName("ImageFrameComposeNonAndroid")

package com.shakster.gifkt.compose

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import org.jetbrains.skia.ColorAlphaType
import org.jetbrains.skia.ColorType
import org.jetbrains.skia.Image
import org.jetbrains.skia.ImageInfo
import kotlin.jvm.JvmName

/**
 * Creates an [ImageBitmap] from [ARGB][argb] data.
 *
 * @param argb The ARGB data.
 * @param width The width of the image.
 * @param height The height of the image.
 *
 * @return An [ImageBitmap].
 *
 * @throws IllegalArgumentException If [width] x [height] is not equal to [argb].[size][IntArray.size].
 */
actual fun createImageBitmap(argb: IntArray, width: Int, height: Int): ImageBitmap {
    checkDimensions(argb, width, height)
    val imageInfo = ImageInfo(
        width = width,
        height = height,
        colorType = ColorType.RGBA_8888,
        alphaType = ColorAlphaType.UNPREMUL,
    )
    val rgba = convertArgbToRgba(argb)
    return Image.makeRaster(
        imageInfo,
        rgba,
        width * imageInfo.bytesPerPixel,
    ).toComposeImageBitmap()
}

private fun checkDimensions(argb: IntArray, width: Int, height: Int) {
    val pixelCount = width * height
    require(pixelCount == argb.size) {
        "width * height must equal argb.size: $width * $height = $pixelCount != ${argb.size}"
    }
}

private fun convertArgbToRgba(argb: IntArray): ByteArray {
    val byteArray = ByteArray(argb.size * Int.SIZE_BYTES)
    argb.forEachIndexed { i, color ->
        val colorIndex = i * Int.SIZE_BYTES
        byteArray[colorIndex] = (color shr 16).toByte()     // Red
        byteArray[colorIndex + 1] = (color shr 8).toByte()  // Green
        byteArray[colorIndex + 2] = color.toByte()          // Blue
        byteArray[colorIndex + 3] = (color shr 24).toByte() // Alpha
    }
    return byteArray
}
