package io.github.shaksternano.gifcodec

import kotlinx.coroutines.channels.Channel

suspend inline fun <E> Channel<E>.forEach(action: (E) -> Unit) {
    for (element in this) {
        action(element)
    }
}

inline fun <E> Channel<E>.forEachCurrent(action: (E) -> Unit) {
    var element = tryReceive().getOrNull()
    while (element != null) {
        action(element)
        element = tryReceive().getOrNull()
    }
}

suspend inline fun <E> Channel<E>.forEachSorted(
    indexSelector: (E) -> Int,
    action: (E) -> Unit,
) {
    val sortedElements = ArrayDeque<IndexedElement<E>>()
    var currentIndex = 0
    forEach { element ->
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

data class IndexedElement<E>(
    val index: Int,
    val element: E,
)
