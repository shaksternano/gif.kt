package com.shakster.gifkt.internal

import androidx.collection.MutableIntSet
import androidx.collection.mutableIntSetOf

internal actual class IntSet actual constructor() {

    private val primitiveSet: MutableIntSet = mutableIntSetOf()

    actual val size: Int
        get() = primitiveSet.size

    actual fun add(value: Int): Boolean = primitiveSet.add(value)

    actual fun forEachIndexed(action: (index: Int, Int) -> Unit) {
        var index = 0
        primitiveSet.forEach { element ->
            action(index++, element)
        }
    }
}
