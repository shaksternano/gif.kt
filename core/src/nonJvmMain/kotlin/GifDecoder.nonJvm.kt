package com.shakster.gifkt

import com.shakster.gifkt.internal.BaseGifDecoder
import com.shakster.gifkt.internal.GifDecoderList
import com.shakster.gifkt.internal.RandomAccessGifDecoderList
import kotlinx.io.IOException
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
 * or [get] in increasing order of their index or timestamp.
 *
 * @throws IOException If an I/O error occurs.
 */
actual class GifDecoder
@Throws(IOException::class)
actual constructor(
    private val data: RandomAccessData,
    private val cacheFrameInterval: Int,
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
     * or [get] in increasing order of their index or timestamp.
     */
    actual constructor(
        bytes: ByteArray,
        cacheFrameInterval: Int,
    ) : this(
        RandomAccessData.of(bytes),
        cacheFrameInterval,
    )

    private val baseDecoder: BaseGifDecoder = BaseGifDecoder(data, cacheFrameInterval)

    /**
     * The width of the GIF in pixels.
     */
    actual val width: Int = baseDecoder.width

    /**
     * The height of the GIF in pixels.
     */
    actual val height: Int = baseDecoder.height

    /**
     * The total number of frames in the GIF.
     */
    actual val frameCount: Int = baseDecoder.frameCount

    /**
     * The total duration of the GIF, which is the sum of all frame durations.
     */
    actual val duration: Duration = baseDecoder.duration

    /**
     * The number of times the GIF is set to loop.
     *
     * A value of 0 means infinite looping.
     *
     * A value of -1 means no looping.
     */
    actual val loopCount: Int = baseDecoder.loopCount

    /**
     * A list of [FrameInfo]s, containing the duration and timestamp of each frame.
     */
    actual val frameInfos: List<FrameInfo>
        get() = baseDecoder.frameInfos

    /**
     * The comment in the GIF comment block metadata.
     * This can be an empty string if no comment is present.
     */
    actual val comment: String = baseDecoder.comment

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
    @Throws(IOException::class)
    actual operator fun get(index: Int): ImageFrame {
        return baseDecoder.readFrame(index)
    }

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
    @Throws(IOException::class)
    actual operator fun get(timestamp: Duration): ImageFrame {
        return baseDecoder.readFrame(timestamp)
    }

    /**
     * Returns a [List] view of all frames in the GIF.
     * The returned list's random access speed depends on the [cacheFrameInterval].
     */
    actual fun asList(): List<ImageFrame> {
        return if (cacheFrameInterval == 1) {
            RandomAccessGifDecoderList(this, baseDecoder)
        } else {
            GifDecoderList(this, baseDecoder)
        }
    }

    /**
     * Returns an [Iterable] view of all frames in the GIF.
     */
    actual fun asIterable(): Iterable<ImageFrame> {
        return asList()
    }

    /**
     * Returns a [Sequence] view of all frames in the GIF.
     */
    actual fun asSequence(): Sequence<ImageFrame> {
        return baseDecoder.asSequence()
    }

    /**
     * Closes the decoder, closing the underlying [data].
     *
     * @throws IOException If an I/O error occurs.
     */
    actual override fun close() {
        baseDecoder.close()
    }

    override fun toString(): String {
        return "GifDecoder(" +
            "data=$data" +
            ", cacheFrameInterval=$cacheFrameInterval" +
            ", width=$width" +
            ", height=$height" +
            ", frameCount=$frameCount" +
            ", duration=$duration" +
            ", loopCount=$loopCount" +
            ", comment=\"$comment\"" +
            ")"
    }
}
