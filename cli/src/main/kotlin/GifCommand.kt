package com.shakster.gifkt.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.terminal
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.help
import com.github.ajalt.clikt.parameters.options.*
import com.github.ajalt.clikt.parameters.types.boolean
import com.github.ajalt.clikt.parameters.types.double
import com.github.ajalt.clikt.parameters.types.int
import com.github.ajalt.clikt.parameters.types.path
import com.shakster.gifkt.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.io.IOException
import java.nio.file.Path
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.microseconds
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.DurationUnit
import kotlin.time.TimeSource

object GifCommand : CliktCommand() {

    private val input: Path by option("--input", "-i")
        .path(
            mustExist = true,
            canBeDir = false,
            mustBeReadable = true,
        )
        .required()
        .help("The input file")

    private val colorDifferenceTolerance: Double by option("--color-difference-tolerance")
        .double()
        .default(0.0)
        .help("The tolerance for color difference used when performing transparency optimization. Set to -1 to disable transparency optimization.")

    private val quantizedColorDifferenceTolerance: Double by option("--quantized-color-difference-tolerance")
        .double()
        .default(-1.0)
        .help("The tolerance for color difference used when performing transparency optimization after quantization. Set to -1 to disable post-quantization transparency optimization.")

    private val loopCount: Int by option("--loop-count")
        .int()
        .default(0)
        .help("The number of times the GIF should loop. Set to 0 for infinite looping. Set to -1 for no looping.")

    private val maxColors: Int by option("--max-colors")
        .int()
        .default(GIF_MAX_COLORS)
        .help("he maximum number of colors in each frame. Must be between 1 and $GIF_MAX_COLORS.")
        .validate {
            require(it in 1..GIF_MAX_COLORS) {
                "Max colors must be between 1 and $GIF_MAX_COLORS."
            }
        }

    private val colorQuantizer: ColorQuantizer by option("--color-quantizer")
        .convert {
            when (it.lowercase()) {
                "neuquant" -> ColorQuantizer.NEU_QUANT
                "octree" -> ColorQuantizer.OCTREE
                else -> {
                    if (it.startsWith("neuquant-")) {
                        val samplingFactor = it.substringAfter("-").toIntOrNull()
                            ?: fail("Invalid neuquant sampling factor: $it")
                        require(samplingFactor in 1..ColorQuantizer.NEU_QUANT_MAX_SAMPLING_FACTOR) {
                            "NeuQuant sampling factor must be between 1 and ${ColorQuantizer.NEU_QUANT_MAX_SAMPLING_FACTOR}."
                        }
                        ColorQuantizer.neuQuant(samplingFactor)
                    } else {
                        fail("Unknown color quantizer: $it")
                    }
                }
            }
        }
        .default(ColorQuantizer.NEU_QUANT)
        .help("The color quantizer to use for reducing the number of colors in each frame to '--max-colors'. Available options: 'neuquant', 'neuquant-<samplingFactor>' where <samplingFactor> is an integer between 1 and ${ColorQuantizer.NEU_QUANT_MAX_SAMPLING_FACTOR}, or 'octree'.")

    private val comment: String by option("--comment")
        .help("An optional comment to include in the GIF comment block metadata.")
        .default("")

    private val alphaFill: Int by option("--alpha-fill")
        .convert { option ->
            option.toIntOrNull()
                ?: option.toIntOrNull(16)
                ?: fail("Invalid alpha fill color: $option. Must be an integer representing an RGB color.")
        }
        .default(-1)
        .help("The solid RGB color to use for filling in pixels with partial alpha transparency, as GIFs do not support partial transparency. Set to -1 to disable filling.")

    private val cropTransparent: Boolean by option("--crop-transparent")
        .boolean()
        .default(true)
        .help("Whether to crop the transparent pixels from the edges of each frame. This can reduce the size of the GIF by a small amount.")

    private val minimumFrameDurationCentiseconds: Int by option("--minimum-frame-duration-centiseconds")
        .int()
        .default(GIF_MINIMUM_FRAME_DURATION_CENTISECONDS)
        .help("The minimum duration for each frame in centiseconds. Setting this to a value less than $GIF_MINIMUM_FRAME_DURATION_CENTISECONDS can result in the GIF being played slower than expected on some GIF viewers.")
        .validate {
            require(it > 0) {
                "Minimum frame duration must be positive."
            }
        }

    private val speed: Double by option("--speed")
        .double()
        .default(1.0)
        .help("The speed of the GIF. A value of 1 is normal speed, 2 is twice as fast, and 0.5 is half as fast.")
        .validate {
            require(it > 0) {
                "Speed must be positive."
            }
        }

    private val maxConcurrency: Int by option("--max-concurrency")
        .int()
        .default(Runtime.getRuntime().availableProcessors())
        .help("The maximum number of frames that can be processed concurrently at the same time.")
        .validate {
            require(it > 0) {
                "Max concurrency must be positive."
            }
        }

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
            val builder = GifEncoder.builder(output)
            builder.colorDifferenceTolerance = colorDifferenceTolerance
            builder.quantizedColorDifferenceTolerance = quantizedColorDifferenceTolerance
            builder.loopCount = loopCount
            builder.maxColors = maxColors
            builder.colorQuantizer = colorQuantizer
            builder.comment = comment
            builder.alphaFill = alphaFill
            builder.cropTransparent = cropTransparent
            builder.minimumFrameDurationCentiseconds = minimumFrameDurationCentiseconds
            builder.maxConcurrency = maxConcurrency
            builder.ioContext = Dispatchers.IO
            val onFrameWrittenCallback = { framesWritten: Int, writtenDuration: Duration ->
                val time = TimeSource.Monotonic.markNow()
                val timeTaken = time - startTime
                val fps = framesWritten / timeTaken.toDouble(DurationUnit.SECONDS)
                val fpsFormatted = String.format("%.2f", fps)
                val progress = framesWritten.toDouble() / imageReader.frameCount
                print("\r${renderProgressBar(progress)} Processed $framesWritten/${imageReader.frameCount} frames, $fpsFormatted FPS")
            }
            if (maxConcurrency == 1) {
                builder.build(onFrameWrittenCallback).use { encoder ->
                    imageReader.readFrames().forEach { imageFrame ->
                        encoder.writeFrame(
                            imageFrame.copy(
                                duration = imageFrame.duration / speed
                            )
                        )
                    }
                }
            } else {
                runBlocking {
                    builder.buildParallel(onFrameWrittenCallback).use { encoder ->
                        imageReader.readFrames().forEach { imageFrame ->
                            encoder.writeFrame(
                                imageFrame.copy(
                                    duration = imageFrame.duration / speed
                                )
                            )
                        }
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
        val progressInt = (progress * total).toInt()
        repeat(total) { index ->
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
