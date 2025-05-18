package io.github.shaksternano.gifcodec.internal

import kotlin.time.Duration

internal data class GifInfo(
    val width: Int,
    val height: Int,
    val globalColorTable: ByteArray?,
    val backgroundColorIndex: Int,
    val frameCount: Int,
    val duration: Duration,
    val loopCount: Int,
    val frames: List<FrameInfo>,
) {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as GifInfo

        if (width != other.width) return false
        if (height != other.height) return false
        if (backgroundColorIndex != other.backgroundColorIndex) return false
        if (frameCount != other.frameCount) return false
        if (loopCount != other.loopCount) return false
        if (!globalColorTable.contentEquals(other.globalColorTable)) return false
        if (duration != other.duration) return false
        if (frames != other.frames) return false

        return true
    }

    override fun hashCode(): Int {
        var result = width
        result = 31 * result + height
        result = 31 * result + backgroundColorIndex
        result = 31 * result + frameCount
        result = 31 * result + loopCount
        result = 31 * result + globalColorTable.contentHashCode()
        result = 31 * result + duration.hashCode()
        result = 31 * result + frames.hashCode()
        return result
    }
}

internal data class FrameInfo(
    val argb: IntArray?,
    val offset: Long,
    val index: Int,
    val timestamp: Duration,
) {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as FrameInfo

        if (offset != other.offset) return false
        if (index != other.index) return false
        if (!argb.contentEquals(other.argb)) return false
        if (timestamp != other.timestamp) return false

        return true
    }

    override fun hashCode(): Int {
        var result = offset.hashCode()
        result = 31 * result + index
        result = 31 * result + (argb?.contentHashCode() ?: 0)
        result = 31 * result + timestamp.hashCode()
        return result
    }
}
