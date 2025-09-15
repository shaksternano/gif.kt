package com.shakster.gifkt.internal

// Can't use typealias because of generics
internal actual class IntIntMap() {

    private val backingMap: MutableMap<Int, Int> = hashMapOf()

    actual operator fun get(key: Int): Int = backingMap.getOrElse(key) {
        throw NoSuchElementException("Key $key not found in map")
    }

    actual operator fun set(key: Int, value: Int) = backingMap.set(key, value)
}

internal actual fun intIntMapOf(): IntIntMap = IntIntMap()
