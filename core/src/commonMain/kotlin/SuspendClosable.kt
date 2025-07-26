package com.shakster.gifkt

import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract
import kotlin.use

/**
 * A resource that can be closed or released asynchronously.
 */
fun interface SuspendClosable {

    /**
     * Closes this resource.
     *
     * This function may throw, thus it is strongly recommended to use the [use] function instead,
     * which closes this resource correctly whether an exception is thrown or not.
     *
     * Implementers of this interface should pay increased attention to cases where the close operation may fail.
     * It is recommended that all underlying resources are closed and the resource internally is marked as closed
     * before throwing an exception. Such a strategy ensures that the resources are released in a timely manner,
     * and avoids many problems that could come up when the resource wraps, or is wrapped, by another resource.
     *
     * Note that calling this function more than once may have some visible side effect.
     * However, implementers of this interface are strongly recommended to make this function idempotent.
     */
    suspend fun close()
}

/**
 * Executes the given [block] function on this resource and then closes it down correctly whether an exception
 * is thrown or not.
 *
 * In case if the resource is being closed due to an exception occurred in [block], and the closing also fails with an exception,
 * the latter is added to the [suppressed][Throwable.addSuppressed] exceptions of the former.
 *
 * @param block a function to process this [AutoCloseable] resource.
 * @return the result of [block] function invoked on this resource.
 */
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
