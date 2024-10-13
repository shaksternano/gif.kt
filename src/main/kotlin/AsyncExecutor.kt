package io.github.shaksternano.gifcodec

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.sync.Semaphore
import kotlin.coroutines.EmptyCoroutineContext

open class AsyncExecutor<T, R>(
    maxConcurrency: Int,
    private val scope: CoroutineScope = CoroutineScope(EmptyCoroutineContext),
    private val task: suspend (T) -> R,
    private val onOutput: suspend (Result<R>) -> Unit,
) : SuspendClosable {

    private val semaphore: Semaphore = Semaphore(maxConcurrency)
    private val outputChannel: Channel<Deferred<Result<R>>> = Channel(maxConcurrency)

    suspend fun submit(input: T) {
        semaphore.acquire()
        val deferred = scope.async {
            try {
                Result.success(task(input))
            } catch (t: Throwable) {
                Result.failure(t)
            } finally {
                semaphore.release()
            }
        }
        outputChannel.send(deferred)
    }

    /**
     * For propagating exceptions.
     */
    suspend fun submitFailure(t: Throwable) {
        outputChannel.send(CompletableDeferred(Result.failure(t)))
    }

    private val outputJob: Job = scope.launch {
        outputChannel.forEach { output ->
            onOutputFunction(output.await())
        }
    }

    protected open suspend fun onOutputFunction(toOutput: Result<R>) {
        onOutput(toOutput)
    }

    override suspend fun close() {
        outputChannel.close()
        outputJob.join()
    }
}
