package com.shakster.gifkt.internal

import com.shakster.gifkt.GifDecoder

internal class JvmRandomAccessGifDecoderList(
    gifDecoder: GifDecoder,
    baseGifDecoder: BaseGifDecoder,
) : JvmGifDecoderList(gifDecoder, baseGifDecoder), RandomAccess
