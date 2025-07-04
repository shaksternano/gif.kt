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

    private val imageReaderFactories: List<ImageReaderFactory> = listOf(
        ::GifImageReader,
        ::JavaxImageReader,
        ::FFmpegImageReader,
    )

    override fun run() {
        val startTime = TimeSource.Monotonic.markNow()
        try {
            getImageReader(input)
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
        println("\nDone in ${timeTaken.toString(DurationUnit.SECONDS, 3)}")
    }

    private fun getImageReader(input: Path): ImageReader {
        var throwable: Throwable? = null
        for (imageReaderFactory in imageReaderFactories) {
            try {
                return imageReaderFactory(input)
            } catch (t: Throwable) {
                throwable = t
            }
        }
        throw IOException(throwable)
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
