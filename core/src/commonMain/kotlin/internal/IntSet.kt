package com.shakster.gifkt.internal

internal expect class IntSet {

    val size: Int

    fun add(element: Int): Boolean
}

// androidx collection constructors have default parameter values which don't work with typealiases
internal expect fun intSetOf(): IntSet

internal expect inline fun IntSet.forEachIndexed(action: (index: Int, Int) -> Unit)
