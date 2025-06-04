package com.shakster.gifkt

import com.shakster.gifkt.internal.DEFAULT_GIF_CACHE_FRAME_INTERVAL
import org.khronos.webgl.ArrayBuffer
import org.khronos.webgl.Int8Array

fun GifDecoder(
    buffer: ArrayBuffer,
    cacheFrameInterval: Int = DEFAULT_GIF_CACHE_FRAME_INTERVAL,
): GifDecoder {
    return GifDecoder(RandomAccessData.of(buffer), cacheFrameInterval)
}

fun GifDecoder(
    bytes: Int8Array,
    cacheFrameInterval: Int = DEFAULT_GIF_CACHE_FRAME_INTERVAL,
): GifDecoder {
    return GifDecoder(RandomAccessData.of(bytes), cacheFrameInterval)
}
