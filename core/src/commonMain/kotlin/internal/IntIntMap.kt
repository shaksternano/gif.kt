package com.shakster.gifkt.internal

internal expect class IntIntMap() {

    operator fun get(key: Int): Int

    fun put(key: Int, value: Int)
}

internal operator fun IntIntMap.set(key: Int, value: Int) {
    put(key, value)
}
