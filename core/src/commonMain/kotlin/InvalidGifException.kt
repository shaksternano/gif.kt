package com.shakster.gifkt

class InvalidGifException(
    message: String,
    cause: Throwable? = null,
) : Exception(message, cause)
