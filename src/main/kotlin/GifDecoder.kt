package io.github.shaksternano.gifcodec

import io.github.shaksternano.gifcodec.internal.*
import kotlinx.io.buffered
import kotlin.time.Duration

/*
 * Reference:
 * https://www.matthewflickinger.com/lab/whatsinagif/bits_and_bytes.asp
 */
class GifDecoder(
    private val data: RandomAccessData,
    private val cacheFrameInterval: Int = 50,
) : AutoCloseable {

    val width: Int
    val height: Int
    val frameCount: Int
    val duration: Duration
    val loopCount: Int

    private val globalColorTable: ByteArray?
    private val globalColorTableColors: Int
    private val backgroundColorIndex: Int
    private val frames: List<FrameInfo>

    init {
        data.read().buffered().use { source ->
            val gifInfo = source.readGif(
                cacheFrameInterval,
                decodeImages = false,
            )

            width = gifInfo.width
            height = gifInfo.height
            frameCount = gifInfo.frameCount
            duration = gifInfo.duration
            loopCount = gifInfo.loopCount
            globalColorTable = gifInfo.globalColorTable
            globalColorTableColors = gifInfo.globalColorTableColors
            backgroundColorIndex = gifInfo.backgroundColorIndex
            frames = gifInfo.frames

            val firstFrame = frames.firstOrNull()
            if (firstFrame != null && firstFrame.timestamp < Duration.ZERO) {
                throw IllegalStateException("First frame timestamp is negative")
            }
        }
    }

    fun readFrame(index: Int): ImageFrame {
        if (frames.isEmpty()) {
            throw NoSuchElementException("No frames available")
        }
        if (index !in frames.indices) {
            throw IndexOutOfBoundsException("Index out of bounds: $index, size: ${frames.size}")
        }

        var image: IntArray? = null
        val keyframe = findLastKeyframe(index)
        for (i in keyframe.index..index) {
            val frame = frames[i]
            if (frame.argb.isNotEmpty()) {
                image = keyframe.argb
            } else {
                data.read(frame.byteOffset).buffered().monitored().use { source ->
                    // Block introducer
                    source.skip(1)
                    val imageData = source.readGifImage(decodeImage = true)
                    val currentColorTable = imageData.localColorTable ?: globalColorTable
                    ?: throw InvalidGifException("Frame $index has no color table")

                    val imageArgb = getImageArgb(
                        width,
                        height,
                        imageData.descriptor.left,
                        imageData.descriptor.top,
                        imageData.descriptor.width,
                        imageData.descriptor.height,
                        imageData.colorIndices,
                        globalColorTable,
                        globalColorTableColors,
                        currentColorTable,
                        backgroundColorIndex,
                        frame.transparentColorIndex,
                        image,
                    )

                    val disposedImage = disposeImage(
                        imageArgb,
                        image,
                        frame.disposalMethod,
                        width,
                        height,
                        imageData.descriptor.left,
                        imageData.descriptor.top,
                        imageData.descriptor.width,
                        imageData.descriptor.height,
                        globalColorTableColors,
                        globalColorTable,
                        currentColorTable,
                        backgroundColorIndex,
                    )
                    if (disposedImage != null) {
                        image = disposedImage
                    }
                }
            }
        }

        if (image == null) {
            throw IllegalStateException("Seeked image is null, this shouldn't happen")
        }
        val targetFrame = frames[index]
        return ImageFrame(
            image,
            width,
            height,
            targetFrame.duration,
            targetFrame.timestamp,
            targetFrame.index,
        )
    }

    private fun findLastKeyframe(index: Int): FrameInfo {
        for (i in index downTo 0) {
            val frame = frames[i]
            if (frame.isKeyFrame || frame.argb.isNotEmpty()) {
                return frame
            }
        }
        throw IllegalStateException("No keyframe found, this should never be reached")
    }

    operator fun get(index: Int): ImageFrame =
        readFrame(index)

    fun readFrame(timestamp: Duration): ImageFrame {
        if (frames.isEmpty()) {
            throw NoSuchElementException("No frames available")
        }
        if (timestamp < Duration.ZERO) {
            throw IllegalArgumentException("Timestamp cannot be negative")
        }
        if (timestamp > duration) {
            throw IllegalArgumentException("Timestamp cannot be greater than duration")
        }

        val index = if (timestamp == frames[0].timestamp) {
            0
        } else if (timestamp < frames[frames.size - 1].timestamp) {
            findIndex(timestamp, frames)
        } else {
            frames.size - 1
        }
        return readFrame(index)
    }

    private fun findIndex(timestamp: Duration, frames: List<FrameInfo>): Int {
        var low = 0
        var high = frames.size - 1
        while (low <= high) {
            val middle = low + (high - low) / 2
            val frameTimestamp = frames[middle].timestamp
            if (frameTimestamp == timestamp
                || (frameTimestamp < timestamp && frames[middle + 1].timestamp > timestamp)
            ) {
                return middle
            } else if (frameTimestamp < timestamp) {
                low = middle + 1
            } else {
                high = middle - 1
            }
        }
        throw IllegalStateException("This should never be reached. Timestamp: $timestamp, frames: $frames")
    }

    operator fun get(timestamp: Duration): ImageFrame =
        readFrame(timestamp)

    fun asSequence(): Sequence<ImageFrame> =
        readGifFrames {
            data.read().buffered()
        }

    override fun close() {
        data.close()
    }
}
