package com.shakster.gifkt.cli

import com.shakster.gifkt.ImageFrame
import org.bytedeco.ffmpeg.global.avutil
import org.bytedeco.javacv.FFmpegFrameGrabber
import org.bytedeco.javacv.Java2DFrameConverter
import java.awt.image.BufferedImage
import java.nio.file.Path
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.sin
import kotlin.time.Duration.Companion.microseconds
import kotlin.time.Duration.Companion.seconds

class FFmpegImageReader(
    input: Path,
) : ImageReader {

    init {
        avutil.av_log_set_level(avutil.AV_LOG_PANIC)
    }

    private val grabber: FFmpegFrameGrabber = FFmpegFrameGrabber(input.toFile()).also {
        it.start()
    }

    override val width: Int = grabber.imageWidth
    override val height: Int = grabber.imageHeight
    override val frameCount: Int = grabber.lengthInFrames

    private val displayRotationRadians: Double = Math.toRadians(-grabber.displayRotation)

    override fun readFrames(): Sequence<ImageFrame> {
        return sequence {
            grabber.setTimestamp(0)
            var frame = grabber.grabImage()
            var index = 0
            val frameDuration = (1 / grabber.frameRate).seconds
            while (frame != null) {
                if (isInvalidImageChannels(frame.imageChannels)) {
                    frame.imageChannels = 3
                }
                val bufferedImage = Java2DFrameConverter().use {
                    it.convert(frame).rotate(displayRotationRadians)
                }
                yield(
                    ImageFrame(
                        bufferedImage,
                        frameDuration,
                        frame.timestamp.microseconds,
                        index,
                    )
                )
                frame.close()
                frame = grabber.grabImage()
                index++
            }
        }
    }

    fun BufferedImage.rotate(radians: Double): BufferedImage {
        if (radians == 0.0) {
            return this
        }

        val sin = abs(sin(radians))
        val cos = abs(cos(radians))

        val resultWidth = floor(width * cos + height * sin).toInt()
        val resultHeight = floor(height * cos + width * sin).toInt()

        val resultType = if (type < BufferedImage.TYPE_INT_RGB || type > BufferedImage.TYPE_BYTE_INDEXED) {
            BufferedImage.TYPE_INT_ARGB
        } else {
            type
        }

        val rotated = BufferedImage(resultWidth, resultHeight, resultType)
        val graphics = rotated.createGraphics()

        graphics.translate((resultWidth - width) / 2, (resultHeight - height) / 2)
        graphics.rotate(radians, width / 2.0, height / 2.0)
        graphics.drawRenderedImage(this, null)
        graphics.dispose()

        return rotated
    }

    private fun isInvalidImageChannels(imageChannels: Int): Boolean {
        return imageChannels != 1 && imageChannels != 3 && imageChannels != 4
    }

    override fun close() {
        grabber.close()
    }
}
