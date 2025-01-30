package io.github.shaksternano.gifcodec

import kotlinx.io.Source
import kotlin.time.Duration

internal fun readGifFrames(sourceSupplier: () -> Source): Sequence<ImageFrame> = sequence {
    sourceSupplier().use { source ->
        readGifSection {
            val introduction = source.readGifIntroduction()
            val canvasWidth = introduction.logicalScreenDescriptor.width
            val canvasHeight = introduction.logicalScreenDescriptor.height
            val canvasSize = canvasWidth * canvasHeight
            val backgroundColorIndex = introduction.logicalScreenDescriptor.backgroundColorIndex
            val globalColorTableColors = introduction.logicalScreenDescriptor.globalColorTableColors
            val globalColorTable = introduction.globalColorTable

            var currentDisposalMethod = DisposalMethod.UNSPECIFIED
            var currentDelayTime = 0
            var currentTransparentColorIndex = -1

            var frameIndex = 0
            var previousImage: IntArray? = null
            var timestamp = Duration.ZERO

            var block = source.readGifBlock(
                decodeImage = true,
                globalColorTableColors = globalColorTableColors,
            )
            while (block != GifTerminator) {
                val frameNumber = frameIndex + 1
                when (block) {
                    is GraphicsControlExtension -> readGifSection("graphics control extension") {
                        currentDisposalMethod = block.disposalMethod
                        currentDelayTime = block.delayTime
                        currentTransparentColorIndex = block.transparentColorIndex
                    }

                    is GifImage -> readGifSection("image $frameNumber") {
                        if (block.data !is DecodedImageData) {
                            throw IllegalStateException("Did not decode image data")
                        }

                        val currentColorTable = block.localColorTable ?: globalColorTable
                        ?: throw InvalidGifException("Frame $frameNumber has no color table")

                        val left = block.descriptor.left
                        val top = block.descriptor.top
                        val width = block.descriptor.width
                        val height = block.descriptor.height
                        val colorIndices = block.data.indices

                        val image = IntArray(canvasSize) { i ->
                            val absoluteX = i % canvasWidth
                            val absoluteY = i / canvasWidth

                            val relativeX = absoluteX - left
                            val relativeY = absoluteY - top

                            val colorIndex = run {
                                if (relativeX in 0..<width && relativeY in 0..<height) {
                                    val index = relativeY * width + relativeX
                                    if (index in colorIndices.indices) {
                                        return@run colorIndices[index].toUByte().toInt()
                                    }
                                }
                                // Missing indices are treated as transparent
                                currentTransparentColorIndex
                            }

                            if (colorIndex == currentTransparentColorIndex) {
                                val finalPreviousImage = previousImage
                                if (finalPreviousImage == null) {
                                    if (
                                        currentColorTable == globalColorTable
                                        && backgroundColorIndex in 0..<globalColorTableColors
                                    ) {
                                        getColor(globalColorTable, backgroundColorIndex)
                                    } else {
                                        // Transparent
                                        0
                                    }
                                } else {
                                    finalPreviousImage[i]
                                }
                            } else {
                                getColor(currentColorTable, colorIndex)
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
                                    // Transparent
                                    0
                                }
                                for (y in left..<height) {
                                    for (x in 0..<width) {
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

                        // Reset values for next frame
                        currentDisposalMethod = DisposalMethod.UNSPECIFIED
                        currentDelayTime = 0
                        currentTransparentColorIndex = -1

                        frameIndex++
                        timestamp += duration
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
}

private fun getColor(colorTable: ByteArray, index: Int): Int {
    val colorIndex = index * BYTES_PER_COLOR
    val red = colorTable[colorIndex].toUByte().toInt()
    val green = colorTable[colorIndex + 1].toUByte().toInt()
    val blue = colorTable[colorIndex + 2].toUByte().toInt()
    val alpha = 0xFF
    return (alpha shl 24) or (red shl 16) or (green shl 8) or blue
}
