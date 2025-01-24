package io.github.shaksternano.gifcodec

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
    val data: ImageData,
) : GifBlock {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as GifImage

        if (descriptor != other.descriptor) return false
        if (!localColorTable.contentEquals(other.localColorTable)) return false
        if (data != other.data) return false

        return true
    }

    override fun hashCode(): Int {
        var result = descriptor.hashCode()
        result = 31 * result + (localColorTable?.contentHashCode() ?: 0)
        result = 31 * result + data.hashCode()
        return result
    }
}

internal data class ImageDescriptor(
    val left: Int,
    val top: Int,
    val width: Int,
    val height: Int,
    val localColorTableBytes: Int,
)

internal data class ImageData(
    val lzwMinimumCodeSize: Int,
    val data: ByteArray,
) {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as ImageData

        if (lzwMinimumCodeSize != other.lzwMinimumCodeSize) return false
        if (!data.contentEquals(other.data)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = lzwMinimumCodeSize
        result = 31 * result + data.contentHashCode()
        return result
    }
}

internal data object GifTerminator : GifBlock
