package com.shakster.gifkt.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.terminal
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.help
import com.github.ajalt.clikt.parameters.options.help
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.types.path
import com.shakster.gifkt.ParallelGifEncoder
import com.shakster.gifkt.use
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.io.IOException
import kotlinx.io.asSink
import kotlinx.io.buffered
import java.nio.file.Path
import kotlin.io.path.outputStream
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.microseconds
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.DurationUnit
import kotlin.time.TimeSource

object GifCommand : CliktCommand() {

    private val input: Path by option("-i", "--input")
        .path(
            mustExist = true,
            canBeDir = false,
            mustBeReadable = true,
        )
        .required()
        .help("The input file")
    private val output: Path by argument()
        .path()
        .help("The output file")

    override fun run() {
        val startTime = TimeSource.Monotonic.markNow()
        try {
            ImageReader.create(input)
        } catch (_: IOException) {
            println("Could not read $input")
            return
        }.use { imageReader ->
            print("${renderProgressBar(0.0)} Processed 0/${imageReader.frameCount} frames, 0 FPS")
            runBlocking {
                ParallelGifEncoder(
                    sink = output.outputStream().asSink().buffered(),
                    transparencyColorTolerance = 0.01,
                    quantizedTransparencyColorTolerance = 0.02,
                    maxConcurrency = Runtime.getRuntime().availableProcessors(),
                    ioContext = Dispatchers.IO,
                ) { framesWritten, writtenDuration ->
                    val progress = framesWritten.toDouble() / imageReader.frameCount
                    val time = TimeSource.Monotonic.markNow()
                    val timeTaken = time - startTime
                    val fps = framesWritten / timeTaken.toDouble(DurationUnit.SECONDS)
                    val fpsFormatted = String.format("%.2f", fps)
                    print("\r${renderProgressBar(progress)} Processed $framesWritten/${imageReader.frameCount} frames, $fpsFormatted FPS")
                }.use { encoder ->
                    imageReader.readFrames().forEach { imageFrame ->
                        encoder.writeFrame(imageFrame)
                    }
                }
            }
        }
        val time = TimeSource.Monotonic.markNow()
        val timeTaken = time - startTime
        val durationUnits = listOf(
            1.days to DurationUnit.DAYS,
            1.hours to DurationUnit.HOURS,
            1.minutes to DurationUnit.MINUTES,
            1.seconds to DurationUnit.SECONDS,
            1.milliseconds to DurationUnit.MILLISECONDS,
            1.microseconds to DurationUnit.MICROSECONDS,
        )
        val timeTakenStringUnit = durationUnits.firstOrNull { timeTaken >= it.first }
            ?.second ?: DurationUnit.NANOSECONDS
        val timeTakenString = timeTaken.toString(timeTakenStringUnit, 3)
        println("\nDone in $timeTakenString")
    }

    private fun renderProgressBar(progress: Double): String {
        var progressBar = "["
        val total = terminal.size.width / 2
        repeat(total) { index ->
            val progressInt = (progress * total).toInt()
            progressBar += if (index < progressInt) {
                "#"
            } else if (index == progressInt) {
                "_"
            } else {
                "."
            }
        }
        progressBar += "]"
        return progressBar
    }
}
