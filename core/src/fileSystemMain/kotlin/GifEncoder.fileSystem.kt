package com.shakster.gifkt

import kotlinx.io.IOException
import kotlinx.io.buffered
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem

/**
 * Creates a new [GifEncoderBuilder] for configuring and building a [GifEncoder].
 *
 * @param path The path of the file to write the GIF data to.
 *
 * @return A [GifEncoderBuilder] instance for configuring the encoder.
 *
 * @throws IOException If an I/O error occurs.
 */
@Suppress("EXTENSION_SHADOWED_BY_MEMBER")
@Throws(IOException::class)
fun GifEncoder.Companion.builder(path: Path): GifEncoderBuilder {
    return GifEncoderBuilder(SystemFileSystem.sink(path).buffered())
}
