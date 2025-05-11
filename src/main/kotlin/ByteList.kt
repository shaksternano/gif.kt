package io.github.shaksternano.gifcodec

import kotlin.math.max

/**
 * A mutable list of primitive [Byte]s.
 * Used for improved performance over a [MutableList] of boxed [Byte]s.
 */
internal class ByteList private constructor(
    private var elements: ByteArray,
    size: Int,
    private var hashCode: Int,
    private var lastHashCode: Int? = null,
) {

    /**
     * The number of elements in the list.
     */
    var size: Int = size
        private set
    val indices: IntRange
        get() = 0..<size

    constructor() : this(
        elements = ByteArray(8),
        size = 0,
        hashCode = 1,
    )

    constructor(element: Byte) : this(
        elements = byteArrayOf(element),
        size = 1,
        hashCode = 31 + element,
    )

    /**
     * Returns the element at the specified [index] in the list.
     */
    operator fun get(index: Int): Byte {
        if (index < 0 || index >= size) {
            throw IndexOutOfBoundsException("Index: $index, Size: $size")
        }
        return elements[index]
    }

    /**
     * Returns the first element in the list.
     */
    fun first(): Byte {
        if (size == 0) {
            throw NoSuchElementException("List is empty.")
        }
        return elements[0]
    }

    /**
     * Adds the specified [element] to the end of this list.
     */
    fun add(element: Byte) {
        if (size == elements.size) {
            elements = getBiggerArray()
        }
        elements[size] = element
        size++
        lastHashCode = hashCode
        hashCode = getNewHashCode(hashCode, element)
    }

    /**
     * Adds the specified [element] to the end of this list.
     */
    operator fun plusAssign(element: Byte) {
        add(element)
    }

    /**
     * Adds all the specified [elements] to the end of this list.
     */
    fun addAll(elements: ByteList) {
        val newSize = size + elements.size
        if (newSize > this.elements.size) {
            this.elements = getBiggerArray(newSize)
        }
        elements.elements.copyInto(
            destination = this.elements,
            destinationOffset = size,
            endIndex = elements.size,
        )
        size = newSize
        for (byte in elements) {
            lastHashCode = hashCode
            hashCode = getNewHashCode(hashCode, byte)
        }
    }

    /**
     * Removes the last element from this mutable list.
     */
    fun removeLast() {
        if (size == 0) {
            throw NoSuchElementException("List is empty.")
        }
        size--
        hashCode = lastHashCode ?: calculateHashCode(elements, size)
        lastHashCode = null
    }

    private fun getBiggerArray(minCapacity: Int = 0): ByteArray {
        val newCapacity = max(getIncreasedCapacity(), minCapacity)
        val newElements = ByteArray(newCapacity)
        elements.copyInto(newElements)
        return newElements
    }

    private fun getIncreasedCapacity(): Int {
        val oldCapacity = elements.size
        return if (oldCapacity > 1) {
            // Floor multiplication by 1.5
            oldCapacity + (oldCapacity shr 1)
        } else {
            // Doesn't work for 0 or 1
            oldCapacity + 1
        }
    }

    /**
     * Removes all the elements from this list.
     */
    fun clear() {
        size = 0
        hashCode = 1
        lastHashCode = null
    }

    /**
     * Returns a new [ByteList] with the same elements as this list.
     */
    fun copyOf(): ByteList =
        ByteList(
            elements = elements.copyOf(size),
            size = size,
            hashCode = hashCode,
        )

    /**
     * Returns a new [ByteArray] with the same elements as this list followed by the specified [element].
     */
    operator fun plus(element: Byte): ByteList {
        val newSize = size + 1
        val newElements = ByteArray(newSize)
        elements.copyInto(
            destination = newElements,
            endIndex = size,
        )
        newElements[size] = element
        return ByteList(
            elements = newElements,
            size = newSize,
            hashCode = getNewHashCode(hashCode, element),
            lastHashCode = hashCode,
        )
    }

    /**
     * Returns a new [ByteArray] with the same elements as this list.
     */
    fun toByteArray(): ByteArray =
        elements.copyOf(size)

    fun decodeToString(): String =
        elements.decodeToString(endIndex = size)

    /**
     * Returns a new [Iterator] over the elements in this list.
     */
    operator fun iterator(): Iterator = Iterator()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as ByteList

        if (size != other.size) return false
        if (!elements.contentEquals(other.elements, size)) return false

        return true
    }

    override fun hashCode(): Int = hashCode

    /**
     * An iterator over the elements in the list.
     */
    inner class Iterator {

        private var index = 0

        operator fun hasNext(): Boolean = index < size

        operator fun next(): Byte {
            if (!hasNext()) {
                throw NoSuchElementException()
            }
            return elements[index++]
        }
    }

    private fun calculateHashCode(elements: ByteArray, size: Int): Int {
        var newHashCode = 1
        repeat(size) { i ->
            newHashCode = 31 * newHashCode + elements[i]
        }
        return newHashCode
    }

    private fun getNewHashCode(currentHashCode: Int, element: Byte): Int =
        31 * currentHashCode + element

    /**
     * Ignores the elements after the specified [size].
     */
    private fun ByteArray.contentEquals(other: ByteArray, size: Int): Boolean {
        if (this === other) return true
        repeat(size) { i ->
            if (this[i] != other[i]) return false
        }
        return true
    }
}
