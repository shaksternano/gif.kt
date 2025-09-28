@file:JvmName("IntSetJvm")

package com.shakster.gifkt.internal

import androidx.collection.MutableIntSet
import androidx.collection.mutableIntSetOf

internal actual typealias IntSet = MutableIntSet

internal actual fun intSetOf(): IntSet = mutableIntSetOf()

internal actual inline fun IntSet.forEachIndexed(action: (index: Int, Int) -> Unit) {
    var index = 0
    forEach { element ->
        action(index++, element)
    }
}
