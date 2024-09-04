package io.github.shaksternano.gifcodec

/**
 * A mutable list of primitive [Byte]s.
 * Used for improved performance over a [MutableList] of boxed [Byte]s.
 */
internal class ByteList private constructor(
    private var elements: ByteArray,
    private var size: Int,
) {

    constructor(element: Byte) : this(
        elements = byteArrayOf(element),
        size = 1,
    )

    /**
     * Adds the specified element to the end of this list.
     */
    fun add(element: Byte) {
        if (size == elements.size) {
            elements = getBiggerArray()
        }
        elements[size] = element
        size++
    }

    /**
     * Returns a new list containing all elements of
     * the original list and then the given [element].
     */
    operator fun plus(element: Byte): ByteList {
        val newElements = if (size == elements.size) {
            getBiggerArray()
        } else {
            elements.copyOf()
        }
        newElements[size] = element
        return ByteList(
            elements = newElements,
            size = size + 1,
        )
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

    /**
     * Removes all the elements from this list.
     */
    fun clear() {
        size = 0
    }

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

    override fun hashCode(): Int {
        var result = 1
        repeat(size) { i ->
            result = 31 * result + elements[i]
        }
        return result
    }
}
