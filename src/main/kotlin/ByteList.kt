package io.github.shaksternano.gifcodec

/**
 * A mutable list of primitive [Byte]s.
 * Used for improved performance over a [MutableList] of boxed [Byte]s.
 */
internal class ByteList private constructor(
    private var elements: ByteArray,
    private var size: Int,
    private var hashCode: Int,
) {

    private var lastHashCode: Int? = null

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

    operator fun get(index: Int): Byte {
        if (index < 0 || index >= size) {
            throw IndexOutOfBoundsException("Index: $index, Size: $size")
        }
        return elements[index]
    }

    /**
     * Adds the specified element to the end of this list.
     */
    fun add(element: Byte) {
        if (size == elements.size) {
            elements = getBiggerArray()
        }
        elements[size] = element
        size++
        lastHashCode = hashCode
        hashCode = getNewHashCode(element)
    }

    operator fun plusAssign(element: Byte) {
        add(element)
    }

    /**
     * Removes the last element from this mutable list.
     */
    fun removeLast() {
        if (size == 0) {
            throw NoSuchElementException("List is empty.")
        }
        size--
        hashCode = lastHashCode ?: recalculateHashCode()
        lastHashCode = null
    }

    private fun recalculateHashCode(): Int {
        var newHashCode = 1
        repeat(size) { i ->
            newHashCode = 31 * newHashCode + elements[i]
        }
        return newHashCode
    }

    private fun getBiggerArray(): ByteArray {
        val newCapacity = getIncreasedCapacity()
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

    private fun getNewHashCode(element: Byte): Int =
        31 * hashCode + element

    /**
     * Removes all the elements from this list.
     */
    fun clear() {
        size = 0
        hashCode = 1
        lastHashCode = null
    }

    fun copyOf(): ByteList =
        ByteList(
            elements = elements.copyOf(size),
            size = size,
            hashCode = hashCode,
        )

    fun toByteArray(): ByteArray =
        elements.copyOf(size)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as ByteList

        if (size != other.size) return false
        if (!elements.contentEquals(other.elements, size)) return false

        return true
    }

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

    override fun hashCode(): Int = hashCode
}
