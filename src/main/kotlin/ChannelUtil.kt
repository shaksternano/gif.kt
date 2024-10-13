package io.github.shaksternano.gifcodec

import kotlinx.coroutines.channels.ReceiveChannel

suspend inline fun <E> ReceiveChannel<E>.forEach(action: (E) -> Unit) {
    for (element in this) {
        action(element)
    }
}

inline fun <E> ReceiveChannel<E>.forEachCurrent(action: (E) -> Unit) {
    var element = tryReceive().getOrNull()
    while (element != null) {
        action(element)
        element = tryReceive().getOrNull()
    }
}
