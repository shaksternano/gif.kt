package com.shakster.gifkt.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.ImageBitmap
import com.shakster.gifkt.ImageFrame

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
expect fun createImageBitmap(argb: IntArray, width: Int, height: Int): ImageBitmap

/**
 * Converts an [ImageFrame] to an [ImageBitmap].
 *
 * @return An [ImageBitmap].
 */
fun ImageFrame.toImageBitmap(): ImageBitmap {
    return createImageBitmap(argb, width, height)
}

/**
 * Remember an [ImageBitmap] for the given [frame].
 * The bitmap is recreated only when the [frame] instance changes.
 *
 * @param frame The source [ImageFrame].
 *
 * @return The remembered [ImageBitmap] for this [frame].
 */
@Composable
fun rememberImageFrameBitmap(frame: ImageFrame): ImageBitmap {
    return remember(frame) {
        frame.toImageBitmap()
    }
}

/**
 * The pixel data of this [ImageBitmap].
 * Each element in the array represents a pixel in ARGB format,
 * going row by row from top to bottom.
 */
inline val ImageBitmap.argb: IntArray
    get() = IntArray(width * height).apply {
        readPixels(this)
    }
