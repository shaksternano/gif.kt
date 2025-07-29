package com.shakster.gifkt

import com.shakster.gifkt.internal.DEFAULT_GIF_CACHE_FRAME_INTERVAL
import kotlinx.io.IOException
import kotlinx.io.files.Path

/**
 * Constructs a GifDecoder, reading GIF data from a file.
 *
 * @param path The path of the file containing the GIF data.
 *
 * @param cacheFrameInterval The interval at which frames are cached.
 * Setting this to a higher value can improve random access speed with [GifDecoder.get],
 * but increases memory usage.
 *
 * Set to 1 to cache every frame, making random access speed similar to that of [Array].
 * Warning: this can cause the decoder to use a large amount of memory.
 *
 * Set to 0 to disable caching, which will decrease the initial load time and minimize memory usage.
 * Disable caching if you only need to read frames sequentially using [GifDecoder.asSequence]
 * or [GifDecoder.get] in order of their index or timestamp.
 *
 * @throws IOException If an I/O error occurs.
 */
@Throws(IOException::class)
fun GifDecoder(
    path: Path,
    cacheFrameInterval: Int = DEFAULT_GIF_CACHE_FRAME_INTERVAL,
): GifDecoder {
    return GifDecoder(path.asRandomAccess(), cacheFrameInterval)
}
