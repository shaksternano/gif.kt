package com.shakster.gifkt.internal

import kotlin.time.Duration

internal data class GifInfo(
    val width: Int,
    val height: Int,
    val frameCount: Int,
    val duration: Duration,
    val loopCount: Int,
    val globalColorTable: ByteArray?,
    val globalColorTableColors: Int,
    val backgroundColorIndex: Int,
    val frames: List<RawImage>,
) {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as GifInfo

        if (width != other.width) return false
        if (height != other.height) return false
        if (frameCount != other.frameCount) return false
        if (duration != other.duration) return false
        if (loopCount != other.loopCount) return false
        if (backgroundColorIndex != other.backgroundColorIndex) return false
        if (!globalColorTable.contentEquals(other.globalColorTable)) return false
        if (frames != other.frames) return false

        return true
    }

    override fun hashCode(): Int {
        var result = width
        result = 31 * result + height
        result = 31 * result + frameCount
        result = 31 * result + duration.hashCode()
        result = 31 * result + loopCount
        result = 31 * result + globalColorTable.contentHashCode()
        result = 31 * result + backgroundColorIndex
        result = 31 * result + frames.hashCode()
        return result
    }
}
