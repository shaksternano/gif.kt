@file:JvmName("ImageFrameComposeAndroid")

package com.shakster.gifkt.compose

import android.graphics.Bitmap
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap

/**
 * Creates an [ImageBitmap] from [ARGB][argb] data.
 *
 * @param argb The ARGB data.
 * @param width The width of the image.
 * @param height The height of the image.
 *
 * @return An [ImageBitmap].
 */
actual fun createImageBitmap(argb: IntArray, width: Int, height: Int): ImageBitmap {
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    bitmap.setPixels(argb, 0, width, 0, 0, width, height)
    return bitmap.asImageBitmap()
}
