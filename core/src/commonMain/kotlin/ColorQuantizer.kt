package com.shakster.gifkt

import com.shakster.gifkt.internal.NeuQuantizer
import com.shakster.gifkt.internal.OctreeQuantizer
import kotlin.jvm.JvmField
import kotlin.jvm.JvmStatic

/**
 * Interface for color quantization algorithms.
 *
 * This interface defines a method to quantize an array of RGB values
 * into a color table with a specified maximum number of colors.
 */
fun interface ColorQuantizer {

    /**
     * Creates a [ColorTable] with a maximum number of colors
     * from the given RGB data.
     *
     * @param rgb The RGB data.
     * Each color is represented by three consecutive bytes in the order of red, green, and blue.
     *
     * @param maxColors The maximum number of colors to include in the color table.
     *
     * @return A [ColorTable] containing the reduced colors.
     */
    fun quantize(rgb: ByteArray, maxColors: Int): ColorTable

    companion object {
        // Using const fails to compile
        /**
         * The maximum sampling factor for the NeuQuant [ColorQuantizer] algorithm.
         */
        @Suppress("MayBeConstant")
        @JvmField
        val NEU_QUANT_MAX_SAMPLING_FACTOR: Int = 30

        /**
         * NeuQuant [ColorQuantizer] with good image quality,
         * using a sampling factor of 10.
         * This quantizer has a good balance between speed and quality,
         */
        @JvmField
        val NEU_QUANT: ColorQuantizer = neuQuant(10)

        /**
         * NeuQuant [ColorQuantizer] with maximum image quality,
         * using a sampling factor of 1.
         * This quantizer is slower but produces the better quality results.
         */
        @JvmField
        val NEU_QUANT_MAX_QUALITY: ColorQuantizer = neuQuant(1)

        /**
         * NeuQuant [ColorQuantizer] with minimum image quality,
         * using a sampling factor of 30.
         * This quantizer is faster but produces lower quality results.
         */
        @JvmField
        val NEU_QUANT_MIN_QUALITY: ColorQuantizer = neuQuant(NEU_QUANT_MAX_SAMPLING_FACTOR)

        /**
         * Octree [ColorQuantizer].
         * This quantizer is fast and results in smaller GIF files,
         * but produces images with low quality.
         */
        @JvmField
        val OCTREE: ColorQuantizer = OctreeQuantizer

        /**
         * Creates a NeuQuant [ColorQuantizer] with the specified sampling factor.
         *
         * @param samplingFactor The sampling factor to use for the NeuQuant algorithm.
         * Lower values result in better image quality at the cost of quantization speed.
         * It must be between 1 and 30.
         *
         * @return A NeuQuant [ColorQuantizer] instance configured with the specified sampling factor.
         */
        @JvmStatic
        fun neuQuant(samplingFactor: Int): ColorQuantizer {
            return NeuQuantizer(samplingFactor.coerceIn(1, NEU_QUANT_MAX_SAMPLING_FACTOR))
        }
    }
}
