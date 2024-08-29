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
    private val onBufferOverflow: (suspend () -> Unit)? = null,
) : SuspendClosable {

    private val semaphore: Semaphore = Semaphore(bufferSize)

    private val inputChannel: Channel<T> = Channel(bufferSize)
    private val outputChannel: Channel<IndexedElement<R>> = Channel(bufferSize)

    suspend fun submit(input: T) {
        if (onBufferOverflow == null) {
            inputChannel.send(input)
        } else {
            // IntelliJ reports a false error without this local variable
            val onBufferOverflow = onBufferOverflow
            var sendFailed = inputChannel.trySend(input).isFailure
            while (sendFailed) {
                onBufferOverflow()
                sendFailed = inputChannel.trySend(input).isFailure
            }
        }
    }

    private val executorJob: Job = scope.launch {
        var index = 0
        for (input in inputChannel) {
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

    private suspend inline fun <E> Channel<E>.forEachSorted(
        indexSelector: (E) -> Int,
        action: (E) -> Unit,
    ) {
        val sortedElements = ArrayDeque<IndexedElement<E>>()
        var currentIndex = 0
        for (element in this) {
            val index = indexSelector(element)
            sortedElements.add(IndexedElement(index, element))
            sortedElements.sortBy { (index, _) ->
                index
            }
            var head = sortedElements.firstOrNull()
            while (head != null && head.index == currentIndex) {
                sortedElements.removeFirst()
                action(head.element)
                head = sortedElements.firstOrNull()
                currentIndex++
            }
        }
    }

    override suspend fun close() {
        inputChannel.close()
        executorJob.join()
        outputChannel.close()
        outputJob.join()
    }

    private data class IndexedElement<E>(
        val index: Int,
        val element: E,
    )
}
