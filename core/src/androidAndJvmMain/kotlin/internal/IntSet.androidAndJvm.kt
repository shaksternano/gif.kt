package com.shakster.gifkt.internal

import org.eclipse.collections.api.factory.primitive.IntSets
import org.eclipse.collections.api.iterator.MutableIntIterator
import org.eclipse.collections.api.set.primitive.MutableIntSet

internal actual class IntSet actual constructor() {

    private val primitiveSet: MutableIntSet = IntSets.mutable.empty()

    actual val size: Int
        get() = primitiveSet.size()

    actual fun add(value: Int): Boolean = primitiveSet.add(value)

    actual operator fun iterator(): IntIterator = object : IntIterator() {

        private val iterator: MutableIntIterator = primitiveSet.intIterator()

        override fun nextInt(): Int = iterator.next()

        override fun hasNext(): Boolean = iterator.hasNext()
    }
}
