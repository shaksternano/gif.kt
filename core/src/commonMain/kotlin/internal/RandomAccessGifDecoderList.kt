package com.shakster.gifkt.internal

import com.shakster.gifkt.GifDecoder

internal class RandomAccessGifDecoderList(
    gifDecoder: GifDecoder,
    baseGifDecoder: BaseGifDecoder,
) : GifDecoderList(gifDecoder, baseGifDecoder), RandomAccess
