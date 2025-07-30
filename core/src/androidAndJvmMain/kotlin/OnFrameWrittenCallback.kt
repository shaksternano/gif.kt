package com.shakster.gifkt

import java.time.Duration

/**
 * A callback that is invoked after a frame is written,
 * providing the number of frames written and the total duration written so far.
 * This can be used to track progress or update a UI.
 */
fun interface OnFrameWrittenCallback {

    /*
     * Has "void" return type instead of "Unit", unlike Kotlin function types,
     * making it easier to use in Java.
     */
    /**
     * Invoked after a frame is written.
     *
     * @param framesWritten The number of frames written so far.
     *
     * @param writtenDuration The total duration of all frames written so far.
     */
    fun onFrameWritten(framesWritten: Int, writtenDuration: Duration)
}
