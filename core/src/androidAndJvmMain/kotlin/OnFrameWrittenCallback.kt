package com.shakster.gifkt

import java.time.Duration

fun interface OnFrameWrittenCallback {

    /*
     * Has "void" return type instead of "Unit", unlike Kotlin function types,
     * making it easier to use in Java.
     */
    fun onFrameWritten(framesWritten: Int, writtenDuration: Duration)
}
