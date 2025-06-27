package com.shakster.gifkt.internal

internal actual class IntSet actual constructor() {

    private val backingSet: MutableSet<Int> = hashSetOf()

    actual val size: Int
        get() = backingSet.size

    actual fun add(value: Int): Boolean = backingSet.add(value)

    actual operator fun iterator(): IntIterator = object : IntIterator() {

        private val iterator = backingSet.iterator()

        override fun nextInt(): Int = iterator.next()

        override fun hasNext(): Boolean = iterator.hasNext()
    }
}
