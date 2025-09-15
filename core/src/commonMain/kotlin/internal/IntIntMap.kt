package com.shakster.gifkt.internal

internal expect class IntIntMap() {

    operator fun get(key: Int): Int

    operator fun set(key: Int, value: Int)
}
