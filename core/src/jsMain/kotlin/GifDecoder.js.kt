package com.shakster.gifkt

import com.shakster.gifkt.internal.DEFAULT_GIF_CACHE_FRAME_INTERVAL
import org.khronos.webgl.ArrayBuffer
import org.khronos.webgl.Int8Array

/**
 * Constructs a GifDecoder, reading GIF data from a [ArrayBuffer].
 *
 * @param buffer The [ArrayBuffer] containing the GIF data.
 *
 * @param cacheFrameInterval The interval at which frames are cached.
 * Setting this to a higher value can improve random access speed with [get],
 * but increases memory usage.
 *
 * Set to 1 to cache every frame, making random access speed similar to that of [Array].
 * Warning: this can cause the decoder to use a large amount of memory.
 *
 * Set to 0 to disable caching, which will decrease the initial load time and minimize memory usage.
 * Disable caching if you only need to read frames sequentially using [asSequence]
 * or [get] in increasing order of their index or timestamp.
 */
fun GifDecoder(
    buffer: ArrayBuffer,
    cacheFrameInterval: Int = DEFAULT_GIF_CACHE_FRAME_INTERVAL,
): GifDecoder {
    return GifDecoder(RandomAccessData.of(buffer), cacheFrameInterval)
}

/**
 * Constructs a GifDecoder, reading GIF data from an [Int8Array].
 *
 * @param bytes The [Int8Array] containing the GIF data.
 *
 * @param cacheFrameInterval The interval at which frames are cached.
 * Setting this to a higher value can improve random access speed with [get],
 * but increases memory usage.
 *
 * Set to 1 to cache every frame, making random access speed similar to that of [Array].
 * Warning: this can cause the decoder to use a large amount of memory.
 *
 * Set to 0 to disable caching, which will decrease the initial load time and minimize memory usage.
 * Disable caching if you only need to read frames sequentially using [asSequence]
 * or [get] in increasing order of their index or timestamp.
 */
fun GifDecoder(
    bytes: Int8Array,
    cacheFrameInterval: Int = DEFAULT_GIF_CACHE_FRAME_INTERVAL,
): GifDecoder {
    return GifDecoder(RandomAccessData.of(bytes), cacheFrameInterval)
}
