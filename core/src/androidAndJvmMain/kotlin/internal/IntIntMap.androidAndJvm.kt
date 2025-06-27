package com.shakster.gifkt.internal

import org.eclipse.collections.api.factory.primitive.IntIntMaps
import org.eclipse.collections.api.map.primitive.MutableIntIntMap

internal actual class IntIntMap actual constructor() {

    private val primitiveMap: MutableIntIntMap = IntIntMaps.mutable.empty()

    actual operator fun get(key: Int): Int = primitiveMap.getOrThrow(key)

    actual fun put(key: Int, value: Int) = primitiveMap.put(key, value)
}
