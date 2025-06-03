package com.shakster.gifkt.internal

import com.shakster.gifkt.GifDecoder
import com.shakster.gifkt.ImageFrame
import java.util.Spliterator

internal open class JvmGifDecoderList(
    gifDecoder: GifDecoder,
    baseGifDecoder: BaseGifDecoder,
) : GifDecoderList(gifDecoder, baseGifDecoder) {

    override fun spliterator(): Spliterator<ImageFrame> {
        return GifDecoderSpliterator(baseGifDecoder)
    }
}
