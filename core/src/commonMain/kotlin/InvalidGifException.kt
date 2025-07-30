package com.shakster.gifkt

/**
 * Exception thrown when a GIF file is invalid and cannot be decoded.
 *
 * @property message The detail message explaining the reason for the exception.
 * @property cause The cause of the exception.
 */
class InvalidGifException(
    message: String,
    cause: Throwable? = null,
) : RuntimeException(message, cause)
