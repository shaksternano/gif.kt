package com.shakster.gifkt.internal

import com.shakster.gifkt.GifDecoder
import com.shakster.gifkt.ImageFrame

internal open class GifDecoderList(
    protected val gifDecoder: GifDecoder,
    protected val baseGifDecoder: BaseGifDecoder,
) : AbstractList<ImageFrame>() {

    override val size: Int = gifDecoder.frameCount

    override fun get(index: Int): ImageFrame {
        return gifDecoder.readFrame(index)
    }

    override fun iterator(): Iterator<ImageFrame> {
        return baseGifDecoder.iterator()
    }

    override fun toString(): String {
        return "GifDecoderList(size=$size, gifDecoder=${gifDecoder})"
    }
}
