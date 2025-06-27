package com.shakster.gifkt.internal

internal expect class IntSet() {

    val size: Int

    fun add(value: Int): Boolean

    operator fun iterator(): IntIterator
}

internal inline fun IntSet.forEachIndexed(action: (index: Int, value: Int) -> Unit) {
    var index = 0
    for (item in this) action(index++, item)
}
