package com.shakster.gifkt

import com.shakster.gifkt.internal.DEFAULT_GIF_CACHE_FRAME_INTERVAL
import kotlinx.io.files.Path

fun GifDecoder(
    path: Path,
    cacheFrameInterval: Int = DEFAULT_GIF_CACHE_FRAME_INTERVAL,
): GifDecoder {
    return GifDecoder(RandomAccessData.of(path), cacheFrameInterval)
}
