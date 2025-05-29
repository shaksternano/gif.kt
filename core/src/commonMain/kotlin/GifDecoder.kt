package com.shakster.gifkt

import com.shakster.gifkt.internal.*
import kotlinx.io.Source
import kotlinx.io.buffered
import kotlinx.io.files.Path
import okio.FileSystem
import kotlin.time.Duration

private const val DEFAULT_CACHE_FRAME_INTERVAL: Int = 50

/*
 * Reference:
 * https://www.matthewflickinger.com/lab/whatsinagif/bits_and_bytes.asp
 */
class GifDecoder(
    private val data: RandomAccessData,
    private val cacheFrameInterval: Int = DEFAULT_CACHE_FRAME_INTERVAL,
) : AutoCloseable {

    constructor(
        path: Path,
        fileSystem: FileSystem,
        cacheFrameInterval: Int = DEFAULT_CACHE_FRAME_INTERVAL,
    ) : this(
        data = FileData(path, fileSystem),
        cacheFrameInterval = cacheFrameInterval,
    )

    constructor(
        bytes: ByteArray,
        cacheFrameInterval: Int = DEFAULT_CACHE_FRAME_INTERVAL,
    ) : this(
        data = ByteArrayData(bytes),
        cacheFrameInterval = cacheFrameInterval,
    )

    val width: Int
    val height: Int
    val frameCount: Int
    val duration: Duration
    val loopCount: Int
    val frameInfos: List<FrameInfo>
        get() = frames.map {
            FrameInfo(
                duration = it.duration,
                timestamp = it.timestamp,
            )
        }
    val comment: String

    private val globalColorTable: ByteArray?
    private val globalColorTableColors: Int
    private val backgroundColorIndex: Int
    private val frames: List<RawImage>

    init {
        val gifInfo = data.read().buffered().use { source ->
            source.readGif(decodeImages = false)
        }

        width = gifInfo.width
        height = gifInfo.height
        frameCount = gifInfo.frameCount
        duration = gifInfo.duration
        loopCount = gifInfo.loopCount
        globalColorTable = gifInfo.globalColorTable
        globalColorTableColors = gifInfo.globalColorTableColors
        backgroundColorIndex = gifInfo.backgroundColorIndex
        frames = gifInfo.frames
        comment = gifInfo.comment

        val firstFrame = frames.firstOrNull()
        if (firstFrame != null && firstFrame.timestamp < Duration.ZERO) {
            throw IllegalStateException("First frame timestamp is negative")
        }
    }

    fun readFrame(index: Int): ImageFrame {
        if (frames.isEmpty()) {
            throw NoSuchElementException("No frames available")
        }
        if (index !in frames.indices) {
            throw IndexOutOfBoundsException("Index out of bounds: $index, size: ${frames.size}")
        }

        val keyframe = findLastKeyframe(index)
        var imageArgb: IntArray? = null
        decodeImages(startIndex = keyframe.index, endIndex = index) { argb, _, _, _ ->
            imageArgb = argb
        }
        if (imageArgb == null) {
            throw IllegalStateException("No images found, this shouldn't happen")
        }

        val targetFrame = frames[index]
        return ImageFrame(
            imageArgb.copyOf(),
            width,
            height,
            targetFrame.duration,
            targetFrame.timestamp,
            targetFrame.index,
        )
    }

    private fun findLastKeyframe(index: Int): RawImage {
        for (i in index downTo 0) {
            val frame = frames[i]
            if (frame.isKeyFrame || frame.argb != null) {
                return frame
            }
        }
        // This should never be reached, but return the first frame just in case.
        return frames.first()
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

    private fun findIndex(timestamp: Duration, frames: List<RawImage>): Int {
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

    fun asSequence(): Sequence<ImageFrame> = sequence {
        decodeImages(startIndex = 0, endIndex = frameCount - 1) { argb, duration, timestamp, index ->
            yield(
                ImageFrame(
                    argb.copyOf(),
                    width,
                    height,
                    duration,
                    timestamp,
                    index,
                )
            )
        }
    }

    private inline fun decodeImages(
        startIndex: Int,
        endIndex: Int,
        onImageDecode: (
            argb: IntArray,
            duration: Duration,
            timestamp: Duration,
            index: Int,
        ) -> Unit,
    ) {
        var previousImageArgb: IntArray? = null
        for (i in startIndex..endIndex) {
            val frame = frames[i]
            val cachedArgb = frame.argb
            val imageArgb = if (cachedArgb == null) {
                val imageData = data.read(frame.byteOffset).buffered().monitored().use { source ->
                    // Block introducer
                    source.skip(1)
                    source.readGifImage(decodeImage = true)
                }
                val currentColorTable = imageData.localColorTable ?: globalColorTable
                ?: throw InvalidGifException("Frame $i has no color table")

                val argb = getImageArgb(
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
                    previousImageArgb,
                )
                if (cacheFrameInterval > 0 && i % cacheFrameInterval == 0) {
                    frame.argb = argb
                }
                argb
            } else {
                cachedArgb
            }

            onImageDecode(
                imageArgb,
                frame.duration,
                frame.timestamp,
                frame.index,
            )

            if (i < endIndex) {
                val disposedImage = disposeImage(
                    imageArgb,
                    previousImageArgb,
                    frame.disposalMethod,
                    width,
                    height,
                    frame.left,
                    frame.top,
                    frame.width,
                    frame.height,
                    usesGlobalColorTable = !frame.usesLocalColorTable,
                    globalColorTable,
                    globalColorTableColors,
                    backgroundColorIndex,
                )
                previousImageArgb = disposedImage
            }
        }
    }

    override fun close() {
        data.close()
    }
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
