package com.shakster.gifkt

import kotlinx.io.buffered
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem

@Suppress("EXTENSION_SHADOWED_BY_MEMBER")
fun GifEncoder.Companion.builder(path: Path): GifEncoderBuilder {
    return GifEncoderBuilder(SystemFileSystem.sink(path).buffered())
}
