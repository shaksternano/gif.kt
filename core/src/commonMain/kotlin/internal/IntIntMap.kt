package com.shakster.gifkt.internal

internal expect class IntIntMap {

    operator fun get(key: Int): Int

    operator fun set(key: Int, value: Int)
}

// androidx collection constructors have default parameter values which don't work with typealiases
internal expect fun intIntMapOf(): IntIntMap
