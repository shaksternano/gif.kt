package com.shakster.gifkt

import com.shakster.gifkt.internal.BaseGifDecoder
import com.shakster.gifkt.internal.GifDecoderList
import com.shakster.gifkt.internal.RandomAccessGifDecoderList
import kotlinx.io.IOException
import kotlin.time.Duration

actual class GifDecoder
@Throws(IOException::class)
actual constructor(
    private val data: RandomAccessData,
    private val cacheFrameInterval: Int,
) : AutoCloseable {

    private val baseDecoder: BaseGifDecoder = BaseGifDecoder(data, cacheFrameInterval)

    actual val width: Int = baseDecoder.width
    actual val height: Int = baseDecoder.height
    actual val frameCount: Int = baseDecoder.frameCount
    actual val duration: Duration = baseDecoder.duration
    actual val loopCount: Int = baseDecoder.loopCount
    actual val frameInfos: List<FrameInfo>
        get() = baseDecoder.frameInfos
    actual val comment: String = baseDecoder.comment

    @Throws(IOException::class)
    actual fun readFrame(index: Int): ImageFrame {
        return baseDecoder.readFrame(index)
    }

    @Throws(IOException::class)
    actual fun readFrame(timestamp: Duration): ImageFrame {
        return baseDecoder.readFrame(timestamp)
    }

    @Throws(IOException::class)
    actual operator fun get(index: Int): ImageFrame {
        return baseDecoder.readFrame(index)
    }

    @Throws(IOException::class)
    actual operator fun get(timestamp: Duration): ImageFrame {
        return baseDecoder.readFrame(timestamp)
    }

    actual fun asList(): List<ImageFrame> {
        return if (cacheFrameInterval == 1) {
            RandomAccessGifDecoderList(this, baseDecoder)
        } else {
            GifDecoderList(this, baseDecoder)
        }
    }

    actual fun asIterable(): Iterable<ImageFrame> {
        return asList()
    }

    actual fun asSequence(): Sequence<ImageFrame> {
        return baseDecoder.asSequence()
    }

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
