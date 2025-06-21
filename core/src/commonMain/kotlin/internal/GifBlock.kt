package com.shakster.gifkt.internal

import com.shakster.gifkt.DisposalMethod

internal sealed interface GifBlock

internal sealed interface GifExtension : GifBlock

internal data class GraphicsControlExtension(
    val disposalMethod: DisposalMethod,
    val delayTime: Int,
    val transparentColorIndex: Int,
) : GifExtension

internal sealed interface ApplicationExtension : GifExtension

internal data class NetscapeApplicationExtension(
    val loopCount: Int,
) : ApplicationExtension

internal data object UnknownApplicationExtension : ApplicationExtension

internal data class CommentExtension(
    val comment: String,
) : GifExtension

internal data object UnknownExtension : GifExtension

internal data class GifImage(
    val descriptor: ImageDescriptor,
    val localColorTable: ByteArray?,
    val colorIndices: ByteList,
) : GifBlock {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as GifImage

        if (descriptor != other.descriptor) return false
        if (!localColorTable.contentEquals(other.localColorTable)) return false
        if (colorIndices != other.colorIndices) return false

        return true
    }

    override fun hashCode(): Int {
        var result = descriptor.hashCode()
        result = 31 * result + (localColorTable?.contentHashCode() ?: 0)
        result = 31 * result + colorIndices.hashCode()
        return result
    }
}

internal data class ImageDescriptor(
    val left: Int,
    val top: Int,
    val width: Int,
    val height: Int,
    val localColorTableColors: Int,
)

internal data object GifTerminator : GifBlock
