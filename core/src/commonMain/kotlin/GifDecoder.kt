package com.shakster.gifkt

import com.shakster.gifkt.internal.DEFAULT_GIF_CACHE_FRAME_INTERVAL
import com.shakster.gifkt.internal.readGif
import kotlinx.io.IOException
import kotlinx.io.Source
import kotlin.time.Duration

/**
 * A class for decoding GIF files.
 * Other than cached frames, configured with [cacheFrameInterval],
 * all frames are decoded only when requested, minimizing memory usage.
 *
 * Basic usage:
 * ```kotlin
 * // Obtain a Path to read the GIF data from
 * val path: Path = ...
 * val data = path.asRandomAccess()
 * val decoder = GifDecoder(data)
 *
 * // Read a single frame by index
 * val frame1 = decoder[0]
 *
 * // Read a single frame by timestamp
 * val frame2 = decoder[2.seconds]
 *
 * // Read all frames
 * decoder.asSequence().forEach { frame ->
 *     // Process each frame
 * }
 *
 * decoder.close()
 * ```
 *
 * @param data The [RandomAccessData] to read the GIF data from.
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
 * or [get] in order of their index or timestamp.
 *
 * @throws IOException If an I/O error occurs.
 */
expect class GifDecoder(
    data: RandomAccessData,
    cacheFrameInterval: Int = DEFAULT_GIF_CACHE_FRAME_INTERVAL,
) : AutoCloseable {

    /**
     * Constructs a GifDecoder, reading GIF data from a byte array.
     *
     * @param bytes The byte array containing the GIF data.
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
     * or [get] in order of their index or timestamp.
     */
    constructor(
        bytes: ByteArray,
        cacheFrameInterval: Int = DEFAULT_GIF_CACHE_FRAME_INTERVAL,
    )

    /**
     * The width of the GIF in pixels.
     */
    val width: Int

    /**
     * The height of the GIF in pixels.
     */
    val height: Int

    /**
     * The total number of frames in the GIF.
     */
    val frameCount: Int

    /**
     * The total duration of the GIF, which is the sum of all frame durations.
     */
    val duration: Duration

    /**
     * The number of times the GIF is set to loop.
     *
     * A value of 0 means infinite looping.
     *
     * A value of -1 means no looping.
     */
    val loopCount: Int

    /**
     * A list of [FrameInfo]s, containing the duration and timestamp of each frame.
     */
    val frameInfos: List<FrameInfo>

    /**
     * The comment in the GIF comment block metadata.
     * This can be an empty string if no comment is present.
     */
    val comment: String

    /**
     * Reads a frame by its index.
     *
     * @param index The index of the frame to read.
     *
     * @return The [ImageFrame] at the specified index.
     *
     * @throws NoSuchElementException if there are no frames available.
     *
     * @throws IndexOutOfBoundsException if the index is out of bounds.
     *
     * @throws IOException If an I/O error occurs.
     */
    operator fun get(index: Int): ImageFrame

    /**
     * Reads a frame by its timestamp.
     *
     * @param timestamp The timestamp of the frame to read.
     *
     * @return The [ImageFrame] at the specified timestamp.
     *
     * @throws NoSuchElementException if there are no frames available.
     *
     * @throws IllegalArgumentException if the timestamp is negative or exceeds the total duration of the GIF.
     *
     * @throws IOException If an I/O error occurs.
     */
    operator fun get(timestamp: Duration): ImageFrame

    /**
     * Returns a [List] view of all frames in the GIF.
     * The returned list's random access speed depends on the [cacheFrameInterval].
     */
    fun asList(): List<ImageFrame>

    /**
     * Returns an [Iterable] view of all frames in the GIF.
     */
    fun asIterable(): Iterable<ImageFrame>

    /**
     * Returns a [Sequence] view of all frames in the GIF.
     */
    fun asSequence(): Sequence<ImageFrame>

    /**
     * Closes the decoder, closing the underlying [data].
     *
     * @throws IOException If an I/O error occurs.
     */
    override fun close()
}

/**
 * Creates a lazy sequence of [ImageFrame]s from the GIF data in this [Source].
 * The sequence decodes the GIF frames on demand, minimizing memory usage.
 * The returned sequence can only be iterated over once.
 *
 * @receiver The [Source] from which to read the GIF data.
 *
 * @return A [Sequence] of [ImageFrame]s.
 */
fun Source.readGif(): Sequence<ImageFrame> {
    return sequence {
        readGif(decodeImages = true) { image ->
            val imageCopy = image.copy(
                argb = image.argb.copyOf(),
            )
            yield(imageCopy)
        }
    }.constrainOnce()
}
