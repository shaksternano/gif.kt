@file:JvmName("ImageFrameComposeAndroid")

package com.shakster.gifkt.compose

import android.graphics.Bitmap
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.shakster.gifkt.argb

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
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    bitmap.argb = argb
    return bitmap.asImageBitmap()
}

private fun checkDimensions(argb: IntArray, width: Int, height: Int) {
    val pixelCount = width * height
    require(pixelCount == argb.size) {
        "width * height must equal argb.size: $width * $height = $pixelCount != ${argb.size}"
    }
}
