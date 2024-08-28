package io.github.shaksternano.gifcodec

import kotlinx.coroutines.channels.Channel

internal suspend inline fun <E> Channel<E>.forEachSorted(
    indexSelector: (E) -> Int,
    action: (E) -> Unit,
) {
    val sortedElements = ArrayDeque<Pair<Int, E>>()
    var currentIndex = 0
    for (element in this) {
        val index = indexSelector(element)
        sortedElements.add(index to element)
        sortedElements.sortBy { (index, _) ->
            index
        }
        val (firstIndex, firstElement) = sortedElements.first()
        if (firstIndex == currentIndex) {
            sortedElements.removeFirst()
            action(firstElement)
            currentIndex++
        }
    }
    sortedElements.forEach { (_, element) ->
        action(element)
    }
}
