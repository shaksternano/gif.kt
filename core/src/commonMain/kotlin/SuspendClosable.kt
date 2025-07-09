package com.shakster.gifkt

import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

fun interface SuspendClosable {

    suspend fun close()
}

suspend inline fun <T : SuspendClosable?, R> T.use(block: (T) -> R): R {
    @OptIn(ExperimentalContracts::class)
    contract {
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
    }
    var exception: Throwable? = null
    try {
        return block(this)
    } catch (e: Throwable) {
        exception = e
        throw e
    } finally {
        closeFinally(exception)
    }
}

@PublishedApi
internal suspend fun SuspendClosable?.closeFinally(cause: Throwable?): Unit = when {
    this == null -> {}
    cause == null -> close()
    else ->
        try {
            close()
        } catch (closeException: Throwable) {
            cause.addSuppressed(closeException)
        }
}
