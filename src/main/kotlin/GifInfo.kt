package io.github.shaksternano.gifcodec

import kotlin.time.Duration

internal data class GifInfo(
    val width: Int,
    val height: Int,
    val globalColorTable: ByteArray?,
    val backgroundColorIndex: Int,
    val frameCount: Int,
    val duration: Duration,
    val loopCount: Int,
    val frameOffsets: List<Long>,
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
        if (frameOffsets != other.frameOffsets) return false

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
        result = 31 * result + frameOffsets.hashCode()
        return result
    }
}
