package com.shakster.gifkt

import com.shakster.gifkt.internal.BaseGifDecoder
import com.shakster.gifkt.internal.JvmGifDecoderList
import com.shakster.gifkt.internal.JvmRandomAccessGifDecoderList
import kotlinx.io.IOException
import kotlin.time.Duration
import kotlin.time.toJavaDuration
import kotlin.time.toKotlinDuration
import java.time.Duration as JavaDuration

actual class GifDecoder
@JvmOverloads
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
    val javaDuration: JavaDuration
        get() = duration.toJavaDuration()
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
    fun readFrame(timestamp: JavaDuration): ImageFrame {
        return baseDecoder.readFrame(timestamp.toKotlinDuration())
    }

    @Throws(IOException::class)
    actual operator fun get(index: Int): ImageFrame {
        return baseDecoder.readFrame(index)
    }

    @Throws(IOException::class)
    actual operator fun get(timestamp: Duration): ImageFrame {
        return baseDecoder.readFrame(timestamp)
    }

    @Throws(IOException::class)
    operator fun get(timestamp: JavaDuration): ImageFrame {
        return baseDecoder.readFrame(timestamp.toKotlinDuration())
    }

    actual fun asList(): List<ImageFrame> {
        return if (cacheFrameInterval == 1) {
            JvmRandomAccessGifDecoderList(this, baseDecoder)
        } else {
            JvmGifDecoderList(this, baseDecoder)
        }
    }

    actual fun asSequence(): Sequence<ImageFrame> {
        return baseDecoder.asSequence()
    }

    @Throws(IOException::class)
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
