package com.shakster.gifkt

/**
 * Interface representing a color table used for color quantization.
 */
interface ColorTable {

    /**
     * The reduced color palette.
     * Each color is represented by three consecutive bytes in the order of red, green, and blue.
     */
    val colors: ByteArray

    /**
     * Returns the index of the closest color in the color table.
     *
     * @param red The red component of the color to find.
     *
     * @param green The green component of the color to find.
     *
     * @param blue The blue component of the color to find.
     *
     * @return The index of the closest color in the color table.
     */
    fun getColorIndex(red: Int, green: Int, blue: Int): Int
}
