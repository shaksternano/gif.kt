package com.shakster.gifkt.cli

import com.shakster.gifkt.ImageFrame
import java.nio.file.Path

interface ImageReader : AutoCloseable {

    val frameCount: Int

    fun readFrames(): Sequence<ImageFrame>
}

typealias ImageReaderFactory = (Path) -> ImageReader
