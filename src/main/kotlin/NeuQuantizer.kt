package io.github.shaksternano.gifcodec

private const val NEU_QUANT_MAX_SAMPLING_FACTOR: Int = 30

class NeuQuantizer(
    quality: Int,
) : ColorQuantizer {

    companion object {
        @JvmField
        val DEFAULT: ColorQuantizer = NeuQuantizer(10)

        @JvmField
        val MAX_QUALITY: ColorQuantizer = NeuQuantizer(1)

        @JvmField
        val MIN_QUALITY: ColorQuantizer = NeuQuantizer(NEU_QUANT_MAX_SAMPLING_FACTOR)
    }

    private val quality: Int = quality.coerceIn(1, NEU_QUANT_MAX_SAMPLING_FACTOR)

    override fun quantize(rgb: ByteArray, maxColors: Int): ColorTable =
        NeuQuantColorTable(NeuQuant(rgb, maxColors, quality))

    private class NeuQuantColorTable(
        private val neuQuant: NeuQuant,
    ) : ColorTable {

        override val colors: ByteArray = neuQuant.process()

        override fun getColorIndex(red: Int, green: Int, blue: Int): Int =
            neuQuant.map(red, green, blue)
    }
}
