package io.github.shaksternano.gifcodec

import kotlinx.io.Source
import kotlinx.io.buffered
import kotlin.time.Duration

/*
 * Reference:
 * https://www.matthewflickinger.com/lab/whatsinagif/bits_and_bytes.asp
 */
class GifDecoder(
    private val data: RandomAccessData,
    private val keyFrameInterval: Int = 50,
) : AutoCloseable {

    val source: Source = data.read().buffered()

    private var initialized: Boolean = false

    private var width: Int = -1
    private var height: Int = -1

    private var globalColorTable: ByteArray? = null
    private var backgroundColorIndex: Int = 0

    private fun init() {
        val introduction = source.readGifIntroduction()
        width = introduction.logicalScreenDescriptor.width
        height = introduction.logicalScreenDescriptor.height
        backgroundColorIndex = introduction.logicalScreenDescriptor.backgroundColorIndex
        globalColorTable = introduction.globalColorTable
        initialized = true
    }

    fun readFrame(index: Int): ImageFrame {
        TODO()
    }

    operator fun get(index: Int): ImageFrame =
        readFrame(index)

    fun readFrame(timestamp: Duration): ImageFrame {
        TODO()
    }

    operator fun get(timestamp: Duration): ImageFrame =
        readFrame(timestamp)

    fun asSequence(): Sequence<ImageFrame> =
        readGifFrames {
            data.read()
        }

    override fun close() {
        source.close()
        data.close()
    }
}
