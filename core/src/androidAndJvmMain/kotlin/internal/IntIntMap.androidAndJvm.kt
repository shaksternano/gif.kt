package com.shakster.gifkt.internal

import androidx.collection.MutableIntIntMap
import androidx.collection.mutableIntIntMapOf

internal actual class IntIntMap actual constructor() {

    private val primitiveMap: MutableIntIntMap = mutableIntIntMapOf()

    actual operator fun get(key: Int): Int = primitiveMap[key]

    actual operator fun set(key: Int, value: Int) = primitiveMap.set(key, value)
}
