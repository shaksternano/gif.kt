package com.shakster.gifkt.internal

internal expect class IntSet() {

    val size: Int

    fun add(value: Int): Boolean

    fun forEachIndexed(action: (index: Int, Int) -> Unit)
}