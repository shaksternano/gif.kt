package io.github.shaksternano.gifcodec

import kotlinx.io.Sink

internal fun Sink.writeByte(byte: Int) =
    writeByte(byte.toByte())
