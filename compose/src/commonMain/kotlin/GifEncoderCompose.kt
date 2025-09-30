package com.shakster.gifkt.compose

import androidx.compose.ui.graphics.ImageBitmap
import com.shakster.gifkt.GifEncoder
import kotlinx.io.IOException
import kotlin.time.Duration

/**
 * Writes a single frame to the GIF.
 * The frame may be skipped if the [duration] is below `minimumFrameDurationCentiseconds`,
 * or if the frame is the same as or similar enough to the previous frame,
 * determined by `colorDifferenceTolerance`, `quantizedColorDifferenceTolerance`,
 * and `colorSimilarityChecker`.
 *
 * @param image The [ImageBitmap] containing the pixel data of the frame.
 *
 * @param duration The duration of the frame.
 *
 * @throws IllegalArgumentException If [duration] is negative.
 *
 * @throws IOException If an I/O error occurs.
 */
@Throws(IOException::class)
fun GifEncoder.writeFrame(image: ImageBitmap, duration: Duration) {
    writeFrame(
        image.argb,
        image.width,
        image.height,
        duration,
    )
}
