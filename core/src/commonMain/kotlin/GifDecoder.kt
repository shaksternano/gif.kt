package com.shakster.gifkt

import com.shakster.gifkt.internal.DEFAULT_GIF_CACHE_FRAME_INTERVAL
import com.shakster.gifkt.internal.readGif
import kotlinx.io.Source
import kotlin.time.Duration

expect class GifDecoder(
    data: RandomAccessData,
    cacheFrameInterval: Int = DEFAULT_GIF_CACHE_FRAME_INTERVAL,
) : AutoCloseable {

    constructor(
        bytes: ByteArray,
        cacheFrameInterval: Int = DEFAULT_GIF_CACHE_FRAME_INTERVAL,
    )

    val width: Int
    val height: Int
    val frameCount: Int
    val duration: Duration
    val loopCount: Int
    val frameInfos: List<FrameInfo>
    val comment: String

    fun readFrame(index: Int): ImageFrame

    fun readFrame(timestamp: Duration): ImageFrame

    operator fun get(index: Int): ImageFrame

    operator fun get(timestamp: Duration): ImageFrame

    fun asList(): List<ImageFrame>

    fun asIterable(): Iterable<ImageFrame>

    fun asSequence(): Sequence<ImageFrame>

    override fun close()
}

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
