@file:JvmName("ByteListJvm")

package com.shakster.gifkt.internal

import java.lang.invoke.MethodHandle
import java.lang.invoke.MethodHandles
import java.lang.invoke.MethodType
import java.util.*

private val EQUALS_HANDLE: MethodHandle? = try {
    val methodType = MethodType.methodType(
        Boolean::class.javaPrimitiveType,
        ByteArray::class.java,
        Int::class.javaPrimitiveType,
        Int::class.javaPrimitiveType,
        ByteArray::class.java,
        Int::class.javaPrimitiveType,
        Int::class.javaPrimitiveType,
    )
    MethodHandles.lookup().findStatic(Arrays::class.java, "equals", methodType)
} catch (_: Throwable) {
    null
}

/**
 * Uses `java.util.Arrays.equals(byte[], int, int, byte[], int, int)` on Java 9 and above.
 */
internal actual fun ByteArray.contentEquals(other: ByteArray, size: Int): Boolean {
    return if (this === other) true
    else if (EQUALS_HANDLE == null) contentEqualsCommon(this, other, size)
    else EQUALS_HANDLE.invokeExact(this, 0, size, other, 0, size) as Boolean
}
