package com.shakster.gifkt.cli

import com.shakster.gifkt.ImageFrame
import org.bytedeco.ffmpeg.global.avutil
import org.bytedeco.javacv.FFmpegFrameGrabber
import org.bytedeco.javacv.Java2DFrameConverter
import java.nio.file.Path
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
                    it.convert(frame)
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

    private fun isInvalidImageChannels(imageChannels: Int): Boolean {
        return imageChannels != 1 && imageChannels != 3 && imageChannels != 4
    }

    override fun close() {
        grabber.close()
    }
}
