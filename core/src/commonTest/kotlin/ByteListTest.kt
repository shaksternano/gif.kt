package com.shakster.gifkt

import com.shakster.gifkt.internal.ByteList
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class ByteListTest {

    @Test
    fun testAdd() {
        val list = ByteList()
        assertEquals(0, list.size)

        list.add(1)
        assertEquals(1, list.size)
        assertEquals(1, list[0])
    }

    @Test
    fun testAddMultiple() {
        val list = ByteList()
        assertEquals(0, list.size)
        for (i in 0..100) {
            list.add(i.toByte())
        }
        assertEquals(101, list.size)

        for (i in 0..100) {
            assertEquals(i.toByte(), list[i])
        }
    }

    @Test
    fun testAddByteListToEmpty() {
        val list1 = ByteList()
        assertEquals(0, list1.size)

        val list2 = ByteList()
        for (i in 0..100) {
            list2.add(i.toByte())
        }
        assertEquals(101, list2.size)

        list1.addAll(list2)

        assertEquals(101, list1.size)
        for (i in 0..100) {
            assertEquals(i.toByte(), list1[i])
        }
    }

    @Test
    fun testAddByteList() {
        val list1 = ByteList()
        for (i in 0..50) {
            list1.add(i.toByte())
        }
        assertEquals(51, list1.size)

        val list2 = ByteList()
        for (i in 51..100) {
            list2.add(i.toByte())
        }
        assertEquals(50, list2.size)

        list1.addAll(list2)

        assertEquals(101, list1.size)
        for (i in 0..100) {
            assertEquals(i.toByte(), list1[i])
        }
    }

    @Test
    fun testAddByteArrayToEmpty() {
        val list = ByteList()
        assertEquals(0, list.size)
        val array = ByteArray(101) { it.toByte() }
        list.addAll(array)
        assertEquals(101, list.size)
        for (i in 0..100) {
            assertEquals(i.toByte(), list[i])
        }
    }

    @Test
    fun testAddByteArrayToNonEmpty() {
        val list = ByteList()
        assertEquals(0, list.size)
        for (i in 0..50) {
            list.add(i.toByte())
        }
        assertEquals(51, list.size)
        val array = ByteArray(50)
        var index = 0
        for (i in 51..100) {
            array[index++] = i.toByte()
        }

        list.addAll(array)
        assertEquals(101, list.size)
        for (i in 0..100) {
            assertEquals(i.toByte(), list[i])
        }
    }

    @Test
    fun testRemoveFromSingle() {
        val list = ByteList()
        assertEquals(0, list.size)
        list.add(1)
        assertEquals(1, list.size)
        list.removeLast()
        assertEquals(0, list.size)
    }

    @Test
    fun testRemoveFromMultiple() {
        val list = ByteList()
        assertEquals(0, list.size)
        for (i in 0..100) {
            list.add(i.toByte())
        }
        assertEquals(101, list.size)
        assertEquals(100, list[list.size - 1])

        list.removeLast()
        assertEquals(100, list.size)
        assertEquals(99, list[list.size - 1])
    }

    @Test
    fun testClear() {
        val list = ByteList()
        assertEquals(0, list.size)
        for (i in 0..100) {
            list.add(i.toByte())
        }
        assertEquals(101, list.size)

        list.clear()
        assertEquals(0, list.size)
    }

    @Test
    fun testCopy() {
        val list = ByteList()
        assertEquals(0, list.size)
        for (i in 0..100) {
            list.add(i.toByte())
        }
        assertEquals(101, list.size)

        val copy = list.copyOf()
        assertEquals(101, copy.size)
        assertEquals(list, copy)
        for (i in 0..100) {
            assertEquals(i.toByte(), copy[i])
        }
    }

    @Test
    fun testPlus() {
        val list1 = ByteList()
        assertEquals(0, list1.size)

        val list2 = list1.plus(1)
        assertEquals(1, list2.size)
        assertEquals(1, list2[0])
    }

    @Test
    fun testPlusMultiple() {
        var list = ByteList()
        assertEquals(0, list.size)

        for (i in 0..100) {
            list = list.plus(i.toByte())
            assertEquals(i + 1, list.size)
            for (j in 0..i) {
                assertEquals(j.toByte(), list[j])
            }
        }
    }

    @Test
    fun testToByteArray() {
        val list = ByteList()
        assertEquals(0, list.size)
        for (i in 0..100) {
            list.add(i.toByte())
        }
        assertEquals(101, list.size)

        val byteArray = list.toByteArray()
        assertEquals(101, byteArray.size)
        for (i in 0..100) {
            assertEquals(i.toByte(), byteArray[i])
        }
    }

    @Test
    fun testIterator() {
        val list = ByteList()
        assertEquals(0, list.size)
        for (i in 0..100) {
            list.add(i.toByte())
        }
        assertEquals(101, list.size)

        var index = 0
        for (byte in list) {
            assertEquals(index.toByte(), byte)
            index++
        }
        assertEquals(101, index)
    }

    @Test
    fun testEquals() {
        val list1 = ByteList()
        assertEquals(0, list1.size)
        for (i in 0..100) {
            list1.add(i.toByte())
        }
        assertEquals(101, list1.size)

        val list2 = ByteList()
        assertEquals(0, list2.size)
        for (i in 0..100) {
            list2.add(i.toByte())
        }
        assertEquals(101, list2.size)

        assertEquals(list1, list2)
    }

    @Test
    fun testNotEquals() {
        val list1 = ByteList()
        assertEquals(0, list1.size)
        for (i in 0..100) {
            list1.add(i.toByte())
        }
        assertEquals(101, list1.size)

        val list2 = ByteList()
        assertEquals(0, list2.size)
        for (i in 100 downTo 0) {
            list2.add(i.toByte())
        }
        assertEquals(101, list2.size)

        assertNotEquals(list1, list2)
    }

    @Test
    fun testHashCode() {
        val list1 = ByteList()
        assertEquals(0, list1.size)
        for (i in 0..100) {
            list1.add(i.toByte())
        }
        assertEquals(101, list1.size)

        val list2 = ByteList()
        assertEquals(0, list2.size)
        for (i in 0..100) {
            list2.add(i.toByte())
        }
        assertEquals(101, list2.size)
        assertEquals(list1.hashCode(), list2.hashCode())

        val set = mutableSetOf<ByteList>()
        set.add(list1)
        assertTrue(set.contains(list2))
    }
}
