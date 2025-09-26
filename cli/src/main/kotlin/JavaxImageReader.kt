package com.shakster.gifkt.cli

import com.shakster.gifkt.ImageFrame
import java.nio.file.Path
import javax.imageio.ImageIO
import kotlin.time.Duration

class JavaxImageReader(
    input: Path,
) : ImageReader {

    private val imageFrame: ImageFrame = ImageFrame(
        image = ImageIO.read(input.toFile()),
        duration = Duration.ZERO,
        timestamp = Duration.ZERO,
        index = 0,
    )

    override val width: Int = imageFrame.width
    override val height: Int = imageFrame.height
    override val frameCount: Int = 1

    override fun readFrames(): Sequence<ImageFrame> {
        return sequenceOf(imageFrame)
    }

    override fun close() = Unit
}
