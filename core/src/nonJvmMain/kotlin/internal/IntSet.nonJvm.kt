package com.shakster.gifkt.internal

internal actual class IntSet actual constructor() {

    private val backingSet: MutableSet<Int> = hashSetOf()

    actual val size: Int
        get() = backingSet.size

    actual fun add(value: Int): Boolean = backingSet.add(value)

    actual fun forEachIndexed(action: (index: Int, Int) -> Unit) {
        backingSet.forEachIndexed(action)
    }
}
