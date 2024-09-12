package io.github.shaksternano.gifcodec

class InvalidGifException(
    message: String,
    cause: Throwable? = null,
) : Exception(message, cause)
