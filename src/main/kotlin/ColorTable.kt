package io.github.shaksternano.gifcodec

interface ColorTable {

    val colors: ByteArray

    fun getColorIndex(red: Int, green: Int, blue: Int): Int
}
