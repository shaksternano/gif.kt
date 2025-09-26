package com.shakster.gifkt.cli

import com.shakster.gifkt.GifDecoder
import com.shakster.gifkt.ImageFrame
import java.nio.file.Path

class GifImageReader(
    input: Path,
) : ImageReader {

    private val decoder = GifDecoder(
        path = input,
        cacheFrameInterval = 0,
    )

    override val width: Int = decoder.width
    override val height: Int = decoder.height
    override val frameCount: Int = decoder.frameCount

    override fun readFrames(): Sequence<ImageFrame> {
        return decoder.asSequence()
    }

    override fun close() {
        decoder.close()
    }
}
