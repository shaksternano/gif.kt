package com.shakster.gifkt.internal

// Can't use typealias because of generics
internal actual class IntSet() {

    internal val backingSet: MutableSet<Int> = hashSetOf()

    actual val size: Int
        get() = backingSet.size

    actual fun add(element: Int): Boolean = backingSet.add(element)
}

internal actual fun intSetOf(): IntSet = IntSet()

internal actual inline fun IntSet.forEachIndexed(action: (index: Int, Int) -> Unit) = backingSet.forEachIndexed(action)
