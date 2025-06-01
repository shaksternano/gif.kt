package com.shakster.gifkt.internal

import com.shakster.gifkt.ImageFrame

internal fun ImageFrame.equalsImpl(other: Any?): Boolean {
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

internal fun ImageFrame.hashCodeImpl(): Int {
    var result = argb.contentHashCode()
    result = 31 * result + width
    result = 31 * result + height
    result = 31 * result + duration.hashCode()
    result = 31 * result + timestamp.hashCode()
    result = 31 * result + index
    return result
}

internal fun ImageFrame.toStringImpl(): String {
    return "ImageFrame(" +
        "argb=${argb.contentToString()}" +
        ", width=$width" +
        ", height=$height" +
        ", duration=$duration" +
        ", timestamp=$timestamp" +
        ", index=$index" +
        ")"
}
