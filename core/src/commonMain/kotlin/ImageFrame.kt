package com.shakster.gifkt

import kotlin.time.Duration

data class ImageFrame(
    val argb: IntArray,
    val width: Int,
    val height: Int,
    val duration: Duration,
    val timestamp: Duration,
    val index: Int,
) {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as ImageFrame

        if (width != other.width) return false
        if (height != other.height) return false
        if (duration != other.duration) return false
        if (timestamp != other.timestamp) return false
        if (index != other.index) return false
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
        return result
    }

    override fun toString(): String {
        return "ImageFrame(" +
            "argb=${argb.contentToString()}" +
            ", width=$width" +
            ", height=$height" +
            ", duration=$duration" +
            ", timestamp=$timestamp" +
            ", index=$index" +
            ")"
    }
}
