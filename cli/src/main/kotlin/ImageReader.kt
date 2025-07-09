package com.shakster.gifkt.cli

import com.shakster.gifkt.ImageFrame
import kotlinx.io.IOException
import java.nio.file.Path

interface ImageReader : AutoCloseable {

    val frameCount: Int

    fun readFrames(): Sequence<ImageFrame>

    companion object {

        private val imageReaderFactories: List<(Path) -> ImageReader> = listOf(
            ::GifImageReader,
            ::JavaxImageReader,
            ::FFmpegImageReader,
        )

        fun create(input: Path): ImageReader {
            var throwable: Throwable? = null
            for (imageReaderFactory in imageReaderFactories) {
                try {
                    return imageReaderFactory(input)
                } catch (t: Throwable) {
                    throwable = t
                }
            }
            throw IOException(throwable)
        }
    }
}
