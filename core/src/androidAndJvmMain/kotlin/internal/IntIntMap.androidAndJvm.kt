@file:JvmName("IntIntMapJvm")

package com.shakster.gifkt.internal

import androidx.collection.MutableIntIntMap
import androidx.collection.mutableIntIntMapOf

internal actual typealias IntIntMap = MutableIntIntMap

internal actual fun intIntMapOf(): IntIntMap = mutableIntIntMapOf()
