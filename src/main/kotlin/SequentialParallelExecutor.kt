package io.github.shaksternano.gifcodec

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlin.coroutines.EmptyCoroutineContext

internal class SequentialParallelExecutor<T, R>(
    bufferSize: Int,
    private val task: suspend (T) -> R,
    private val onOutput: suspend (R) -> Unit,
    scope: CoroutineScope = CoroutineScope(EmptyCoroutineContext),
) : SuspendClosable, CoroutineScope by scope {

    private val inputChannel: Channel<T> = Channel(bufferSize)
    private val outputChannel: Channel<IndexedElement<R>> = Channel(bufferSize)

    suspend fun submit(input: T) {
        inputChannel.send(input)
    }

    private val executorJob: Job = launch {
        var index = 0
        for (input in inputChannel) {
            val finalIndex = index
            launch {
                val output = task(input)
                outputChannel.send(IndexedElement(finalIndex, output))
            }
            index++
        }
    }

    private val outputJob: Job = launch {
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
