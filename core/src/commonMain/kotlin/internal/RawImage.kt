package com.shakster.gifkt.internal

import kotlin.time.Duration

internal data class RawImage(
    var argb: IntArray,
    val left: Int,
    val top: Int,
    val width: Int,
    val height: Int,
    val usesLocalColorTable: Boolean,
    val transparentColorIndex: Int,
    val disposalMethod: DisposalMethod,
    val duration: Duration,
    val timestamp: Duration,
    val index: Int,
    val byteOffset: Long,
    val isKeyFrame: Boolean,
) {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as RawImage

        if (width != other.width) return false
        if (height != other.height) return false
        if (duration != other.duration) return false
        if (timestamp != other.timestamp) return false
        if (index != other.index) return false
        if (byteOffset != other.byteOffset) return false
        if (!argb.contentEquals(other.argb)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = argb.contentHashCode()
        result = 31 * result + width
        result = 31 * result + height
        result = 31 * result + duration.hashCode()
        result = 31 * result + timestamp.hashCode()
        result = 31 * result + index
        result = 31 * result + byteOffset.hashCode()
        return result
    }
}
