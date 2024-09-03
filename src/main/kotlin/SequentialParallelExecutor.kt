package io.github.shaksternano.gifcodec

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlin.coroutines.EmptyCoroutineContext

internal class SequentialParallelExecutor<T, R>(
    bufferSize: Int,
    scope: CoroutineScope = CoroutineScope(EmptyCoroutineContext),
    private val task: suspend (T) -> R,
    private val onOutput: suspend (R) -> Unit,
) : SuspendClosable {

    private val semaphore: Semaphore = Semaphore(bufferSize)
    private val inputChannel: Channel<T> = Channel(bufferSize)
    private val outputChannel: Channel<IndexedElement<R>> = Channel(bufferSize)

    suspend fun submit(input: T) {
        inputChannel.send(input)
    }

    private val executorJob: Job = scope.launch {
        var index = 0
        inputChannel.forEach { input ->
            val finalIndex = index
            semaphore.acquire()
            launch {
                try {
                    val output = task(input)
                    outputChannel.send(IndexedElement(finalIndex, output))
                } finally {
                    semaphore.release()
                }
            }
            index++
        }
    }

    private val outputJob: Job = scope.launch {
        outputChannel.forEachSorted(IndexedElement<R>::index) { (_, output) ->
            onOutput(output)
        }
    }

    override suspend fun close() {
        inputChannel.close()
        executorJob.join()
        outputChannel.close()
        outputJob.join()
    }
}
