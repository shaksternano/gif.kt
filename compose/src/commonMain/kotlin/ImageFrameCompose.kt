package com.shakster.gifkt.compose

import androidx.compose.ui.graphics.ImageBitmap
import com.shakster.gifkt.ImageFrame

/**
 * Converts an [ImageFrame] to an [ImageBitmap].
 *
 * @return An [ImageBitmap].
 */
fun ImageFrame.toImageBitmap(): ImageBitmap {
    return createImageBitmap(argb, width, height)
}

/**
 * Creates an [ImageBitmap] from [ARGB][argb] data.
 *
 * @param argb The ARGB data.
 * @param width The width of the image.
 * @param height The height of the image.
 *
 * @return An [ImageBitmap].
 */
expect fun createImageBitmap(argb: IntArray, width: Int, height: Int): ImageBitmap
