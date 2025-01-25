package io.github.shaksternano.gifcodec

import kotlinx.io.Source
import kotlin.time.Duration

internal fun readGifFrames(sourceSupplier: () -> Source): Sequence<ImageFrame> = sequence {
    sourceSupplier().use { source ->
        val introduction = source.readGifIntroduction()
        val canvasWidth = introduction.logicalScreenDescriptor.width
        val canvasHeight = introduction.logicalScreenDescriptor.height
        val backgroundColorIndex = introduction.logicalScreenDescriptor.backgroundColorIndex
        val globalColorTableColors = introduction.logicalScreenDescriptor.globalColorTableColors
        val globalColorTable = introduction.globalColorTable

        var currentDisposalMethod = DisposalMethod.UNSPECIFIED
        var currentDelayTime = 0
        var currentTransparentColorIndex = -1

        var currentColorTable = globalColorTable
        var frameIndex = 0
        var previousImage: IntArray? = null
        var timestamp = Duration.ZERO

        var block = source.readGifBlock(
            decodeImage = true,
            globalColorTableColors = globalColorTableColors,
        )
        while (block != GifTerminator) {
            when (block) {
                is GraphicsControlExtension -> {
                    currentDisposalMethod = block.disposalMethod
                    currentDelayTime = block.delayTime
                    currentTransparentColorIndex = block.transparentColorIndex
                }

                is GifImage -> {
                    if (block.data !is DecodedImageData) {
                        throw IllegalStateException("Did not decode image data")
                    }
                    if (block.localColorTable != null) {
                        currentColorTable = block.localColorTable
                    }
                    if (currentColorTable == null) {
                        throw InvalidGifException("Frame ${frameIndex + 1} has no color table")
                    }

                    val left = block.descriptor.left
                    val top = block.descriptor.top
                    val width = block.descriptor.width
                    val height = block.descriptor.height
                    val indices = block.data.indices

                    val image = IntArray(canvasWidth * canvasHeight)
                    if (
                        left == 0
                        && top == 0
                        && width == canvasWidth
                        && height == canvasHeight
                    ) {
                        if (indices.size < image.size) {
                            throw InvalidGifException("Frame ${frameIndex + 1} has too few indices")
                        }
                        // Ignore indices that are out of bounds
                        repeat(image.size) { i ->
                            val colorIndex = indices[i].toInt()
                            val pixel = getPixel(
                                colorIndex,
                                i,
                                currentTransparentColorIndex,
                                previousImage,
                                currentColorTable,
                                globalColorTableColors,
                                globalColorTable,
                                backgroundColorIndex,
                            )
                            image[i] = pixel
                        }
                    } else {
                        previousImage?.copyInto(image)
                        for (y in 0 until height) {
                            for (x in 0 until width) {
                                val absoluteX = left + x
                                val absoluteY = top + y
                                if (absoluteX >= canvasWidth || absoluteY >= canvasHeight) {
                                    continue
                                }
                                val i = y * width + x
                                val colorIndex = indices[i].toInt()
                                val imageIndex = absoluteY * canvasWidth + absoluteX
                                val pixel = getPixel(
                                    colorIndex,
                                    imageIndex,
                                    currentTransparentColorIndex,
                                    previousImage,
                                    currentColorTable,
                                    globalColorTableColors,
                                    globalColorTable,
                                    backgroundColorIndex,
                                )
                                image[imageIndex] = pixel
                            }
                        }
                    }

                    val duration = currentDelayTime.centiseconds
                    val imageFrame = ImageFrame(
                        image,
                        canvasWidth,
                        canvasHeight,
                        duration,
                        timestamp,
                        frameIndex,
                    )
                    yield(imageFrame)

                    when (currentDisposalMethod) {
                        DisposalMethod.UNSPECIFIED -> previousImage = image
                        DisposalMethod.DO_NOT_DISPOSE -> previousImage = image
                        DisposalMethod.RESTORE_TO_BACKGROUND_COLOR -> run {
                            if (previousImage == null) return@run
                            val backgroundColor = if (
                                currentColorTable == globalColorTable
                                && backgroundColorIndex in 0..<globalColorTableColors
                            ) {
                                getColor(globalColorTable, backgroundColorIndex)
                            } else {
                                0
                            }
                            for (y in left until height) {
                                for (x in 0 until width) {
                                    val absoluteX = left + x
                                    val absoluteY = top + y
                                    if (absoluteX >= canvasWidth || absoluteY >= canvasHeight) {
                                        continue
                                    }
                                    val i = absoluteY * canvasWidth + absoluteX
                                    previousImage[i] = backgroundColor
                                }
                            }
                        }

                        DisposalMethod.RESTORE_TO_PREVIOUS -> Unit
                    }

                    timestamp += duration
                    frameIndex++
                }

                else -> Unit
            }

            block = source.readGifBlock(
                decodeImage = true,
                globalColorTableColors = globalColorTableColors,
            )
        }
    }
}

private fun getPixel(
    colorIndex: Int,
    imageIndex: Int,
    currentTransparentColorIndex: Int,
    previousImage: IntArray?,
    currentColorTable: ByteArray,
    globalColorTableColors: Int,
    globalColorTable: ByteArray?,
    backgroundColorIndex: Int,
): Int {
    return if (colorIndex == currentTransparentColorIndex) {
        if (previousImage == null) {
            if (
                currentColorTable == globalColorTable
                && backgroundColorIndex in 0..<globalColorTableColors
            ) {
                getColor(globalColorTable, backgroundColorIndex)
            } else {
                0
            }
        } else {
            previousImage[imageIndex]
        }
    } else {
        getColor(currentColorTable, colorIndex)
    }
}

private fun getColor(colorTable: ByteArray, index: Int): Int {
    val colorIndex = index * BYTES_PER_COLOR
    val red = colorTable[colorIndex].toUByte().toInt()
    val green = colorTable[colorIndex + 1].toUByte().toInt()
    val blue = colorTable[colorIndex + 2].toUByte().toInt()
    val alpha = 0xFF
    return (alpha shl 24) or (red shl 16) or (green shl 8) or blue
}
