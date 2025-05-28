package com.shakster.gifkt

interface ColorTable {

    val colors: ByteArray

    fun getColorIndex(red: Int, green: Int, blue: Int): Int
}
