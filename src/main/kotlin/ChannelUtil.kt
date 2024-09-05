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
