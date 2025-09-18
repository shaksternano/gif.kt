package com.shakster.gifkt

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

class TestGifDecoder {

    @Test
    fun testReadStaticGif() {
        val gifFrames = readGifFrames("media/square/square.gif")
        assertEquals(1, gifFrames.size)
        val frame = gifFrames.first()
        val expectedRgb = loadImage("media/square/square.png").rgb
        assertContentEquals(expectedRgb, frame.argb)
        assertEquals(10, frame.width)
        assertEquals(10, frame.height)
        assertEquals(Duration.ZERO, frame.duration)
        assertEquals(Duration.ZERO, frame.timestamp)
        assertEquals(0, frame.index)
    }

    @Test
    fun testReadLargeStaticGif() {
        val gifFrames = readGifFrames("media/dancing/dancing.gif")
        assertEquals(1, gifFrames.size)
        val frame = gifFrames.first()
        val expectedRgb = loadImage("media/dancing/dancing.png").rgb
        assertContentEquals(expectedRgb, frame.argb)
        assertEquals(2357, frame.width)
        assertEquals(2361, frame.height)
        assertEquals(Duration.ZERO, frame.duration)
        assertEquals(Duration.ZERO, frame.timestamp)
        assertEquals(0, frame.index)
    }

    @Test
    fun testReadAnimatedGif() {
        val gifFrames = readGifFrames("media/trafficlight/traffic-light.gif")
        assertEquals(3, gifFrames.size)
        val expectedFrameDurations = listOf(1.seconds, 0.5.seconds, 1.seconds)
        var expectedTimestamp = Duration.ZERO
        gifFrames.forEachIndexed { i, frame ->
            val frameNumber = i + 1
            val expectedRgb = loadImage("media/trafficlight/traffic-light-frame-$frameNumber.png").rgb
            val expectedDuration = expectedFrameDurations[i]
            assertContentEquals(expectedRgb, frame.argb, "Frame $frameNumber")
            assertEquals(11, frame.width)
            assertEquals(29, frame.height)
            assertEquals(expectedDuration, frame.duration)
            assertEquals(expectedTimestamp, frame.timestamp)
            assertEquals(i, frame.index)
            expectedTimestamp += expectedDuration
        }
    }

    @Test
    fun testCorrectTransparency() {
        val gifFrames = readGifFrames("media/dancecrazy/dance-crazy.gif")
        assertEquals(2, gifFrames.size)
        val expectedFrameDurations = listOf(30.milliseconds, 30.milliseconds)
        var expectedTimestamp = Duration.ZERO
        gifFrames.forEachIndexed { i, frame ->
            val frameNumber = i + 1
            val expectedRgb = loadImage("media/dancecrazy/dance-crazy-frame-$frameNumber.png").rgb
            val expectedDuration = expectedFrameDurations[i]
            assertContentEquals(expectedRgb, frame.argb, "Frame $frameNumber")
            assertEquals(84, frame.width)
            assertEquals(128, frame.height)
            assertEquals(expectedDuration, frame.duration)
            assertEquals(expectedTimestamp, frame.timestamp)
            assertEquals(i, frame.index)
            expectedTimestamp += expectedDuration
        }
    }
}
