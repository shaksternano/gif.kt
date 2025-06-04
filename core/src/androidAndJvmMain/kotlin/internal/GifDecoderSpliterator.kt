package com.shakster.gifkt.internal

import com.shakster.gifkt.ImageFrame
import java.util.*
import java.util.function.Consumer

internal class GifDecoderSpliterator(
    private val baseGifDecoder: BaseGifDecoder,
    private var index: Int = 0,
    private var endIndex: Int = baseGifDecoder.frameCount - 1,
) : Spliterator<ImageFrame> {

    private val remaining: Int
        get() = endIndex - index + 1

    private val iterator: Iterator<ImageFrame> = baseGifDecoder.iterator(
        startIndex = index,
        endIndex = endIndex,
    )

    override fun tryAdvance(action: Consumer<in ImageFrame>): Boolean {
        if (index > endIndex || !iterator.hasNext()) {
            return false
        }
        val frame = iterator.next()
        index++
        action.accept(frame)
        return true
    }

    override fun forEachRemaining(action: Consumer<in ImageFrame>) {
        if (index > endIndex) {
            return
        }
        iterator.forEach {
            index++
            action.accept(it)
        }
    }

    override fun trySplit(): Spliterator<ImageFrame>? {
        val remaining = remaining
        if (remaining <= 1) {
            return null
        }
        val middle = index + (remaining ushr 1) // Divide by 2
        val end = endIndex
        endIndex = middle - 1
        return GifDecoderSpliterator(baseGifDecoder, middle, end)
    }

    override fun estimateSize(): Long {
        return remaining.toLong()
    }

    override fun getExactSizeIfKnown(): Long {
        return remaining.toLong()
    }

    override fun characteristics(): Int {
        return Spliterator.ORDERED or
            Spliterator.DISTINCT or
            Spliterator.SORTED or
            Spliterator.SIZED or
            Spliterator.NONNULL or
            Spliterator.IMMUTABLE or
            Spliterator.SUBSIZED
    }

    override fun getComparator(): Comparator<in ImageFrame>? {
        return null
    }
}
