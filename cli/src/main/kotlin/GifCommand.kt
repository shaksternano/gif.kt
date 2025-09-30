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
import com.sksamuel.scrimage.ImmutableImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import java.nio.file.Files
import java.nio.file.Path
import java.util.*
import kotlin.io.path.isDirectory
import kotlin.io.path.isRegularFile
import kotlin.streams.asSequence
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
            mustBeReadable = true,
        )
        .required()
        .help("The input file or directory. If a directory is specified, each file in the directory will be read in alphabetical order of filenames.")

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
                "Max colors must be between 1 and $GIF_MAX_COLORS inclusive."
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
        .help("The color quantizer to use for reducing the number of colors in each frame to '--max-colors'. Available options: 'neuquant', 'neuquant-<samplingFactor>' (where <samplingFactor> is an integer between 1 and ${ColorQuantizer.NEU_QUANT_MAX_SAMPLING_FACTOR}), or 'octree'.")

    private val colorSimilarityChecker: ColorSimilarityChecker by option("--color-similarity-checker")
        .convert {
            when (it.lowercase()) {
                "euclidean" -> ColorSimilarityChecker.EUCLIDEAN
                "euclidean-luminance" -> ColorSimilarityChecker.EUCLIDEAN_LUMINANCE_WEIGHTING
                "cielab" -> ColorSimilarityChecker.CIELAB
                else -> {
                    if (it.startsWith("euclidean-")) {
                        val weights = it.substringAfter("-").split(",").map { weight ->
                            weight.toDoubleOrNull()
                                ?: fail("Invalid euclidean weights: $it. Must be three comma-separated doubles.")
                        }
                        require(weights.size == 3) {
                            "Euclidean weights must be three comma-separated doubles."
                        }
                        ColorSimilarityChecker.euclidean(weights[0], weights[1], weights[2])
                    } else {
                        fail("Unknown color similarity checker: $it")
                    }
                }
            }
        }
        .default(ColorSimilarityChecker.EUCLIDEAN_LUMINANCE_WEIGHTING)
        .help("The color similarity checker to use for determining if two frames are similar enough to merge. Available options: 'euclidean', 'euclidean-luminance-weighting', 'euclidean-<redWeight>,<greenWeight>,<blueWeight>' (where <redWeight>, <greenWeight>, and <blueWeight> are numbers representing the weights for the red, green, and blue channels), or 'cielab'.")

    private val comment: String by option("--comment")
        .default("")
        .help("An optional comment to include in the GIF comment block metadata.")

    private val transparentAlphaThreshold: Int by option("--transparent-alpha-threshold")
        .int()
        .default(20)
        .help("The alpha threshold for a pixel to be considered transparent. Pixels with an alpha value equal to or less than this value will be treated as fully transparent. Must be between 0 and 255 inclusive.")
        .validate {
            require(it in 0..255) {
                "Transparent alpha threshold must be between 0 and 255 inclusive."
            }
        }

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

    private val width: Int? by option("--width", "-w")
        .int()
        .help("The target width of the output GIF. If used with --height, the output will be scaled as large as possible to fit into the specified dimensions while maintaining the aspect ratio.")
        .validate {
            require(it > 0) {
                "Width must be positive."
            }
        }

    private val height: Int? by option("--height", "-h")
        .int()
        .help("The maximum height of the output GIF. If used with --width, the output will be scaled as large as possible to fit into the specified dimensions while maintaining the aspect ratio.")
        .validate {
            require(it > 0) {
                "Height must be positive."
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

    private val imageDuration: Double by option("--image-duration")
        .double()
        .default(1.0)
        .help("If the input is a directory, this is the duration in seconds to assign to each static image in the directory.")
        .validate {
            require(it > 0) {
                "Image durations must be positive."
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

        val imageReaders = if (input.isDirectory()) {
            Files.list(input)
                .asSequence()
                .filter {
                    it.isRegularFile()
                }
                .sortedBy {
                    it.fileName.toString()
                }
                .mapNotNull {
                    try {
                        ImageReader.create(it)
                    } catch (_: Throwable) {
                        println("Could not read $it, skipping")
                        null
                    }
                }
                .toList()
                .ifEmpty {
                    println("Could not read any files in directory $input")
                    return
                }
        } else {
            listOf(ImageReader.create(input))
        }

        val targetWidth = imageReaders.maxOf { it.width }
        val targetHeight = imageReaders.maxOf { it.height }
        val totalFrames = imageReaders.sumOf { it.frameCount }

        print("${renderProgressBar(0.0)} Processed 0/$totalFrames frames, 0 FPS")

        val builder = GifEncoder.builder(output)
        builder.colorDifferenceTolerance = colorDifferenceTolerance
        builder.quantizedColorDifferenceTolerance = quantizedColorDifferenceTolerance
        builder.loopCount = loopCount
        builder.maxColors = maxColors
        builder.colorQuantizer = colorQuantizer
        builder.colorSimilarityChecker = colorSimilarityChecker
        builder.comment = comment
        builder.transparentAlphaThreshold = transparentAlphaThreshold
        builder.alphaFill = alphaFill
        builder.cropTransparent = cropTransparent
        builder.minimumFrameDurationCentiseconds = minimumFrameDurationCentiseconds
        builder.maxConcurrency = maxConcurrency
        builder.ioContext = Dispatchers.IO

        val onFrameWrittenCallback = { framesWritten: Int, _: Duration ->
            val time = TimeSource.Monotonic.markNow()
            val timeTaken = time - startTime
            val fps = framesWritten / timeTaken.toDouble(DurationUnit.SECONDS)
            val fpsFormatted = String.format(Locale.ROOT, "%.2f", fps)
            val progress = framesWritten.toDouble() / totalFrames
            print("\r${renderProgressBar(progress)} Processed $framesWritten/$totalFrames frames, $fpsFormatted FPS")
        }

        val frames = imageReaders.asSequence()
            .flatMap { reader ->
                sequence {
                    reader.use {
                        yieldAll(reader.readFrames().map {
                            if (reader.frameCount > 1) {
                                it
                            } else {
                                it.copy(
                                    duration = imageDuration.seconds,
                                )
                            }
                        })
                    }
                }
            }
            .map { fitImageFrame(it, targetWidth, targetHeight) }
            .map(::transformImageFrame)

        try {
            if (maxConcurrency == 1) {
                builder.build(onFrameWrittenCallback).use { encoder ->
                    frames.forEach { imageFrame ->
                        encoder.writeFrame(transformImageFrame(imageFrame))
                    }
                }
            } else {
                runBlocking {
                    builder.buildParallel(onFrameWrittenCallback).use { encoder ->
                        frames.forEach { imageFrame ->
                            encoder.writeFrame(transformImageFrame(imageFrame))
                        }
                    }
                }
            }
        } finally {
            imageReaders.forEach {
                runCatching {
                    it.close()
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

    private fun fitImageFrame(imageFrame: ImageFrame, targetWidth: Int, targetHeight: Int): ImageFrame {
        return if (imageFrame.width == targetWidth && imageFrame.height == targetHeight) {
            imageFrame
        } else {
            val resized = ImmutableImage.wrapAwt(imageFrame.toBufferedImage())
                .fit(targetWidth, targetHeight)
            ImageFrame(
                image = resized.awt(),
                duration = imageFrame.duration,
                timestamp = imageFrame.timestamp,
                index = imageFrame.index,
            )
        }
    }

    private fun transformImageFrame(imageFrame: ImageFrame): ImageFrame {
        val width = width
        val height = height

        val resizeWidth = width != null && width != imageFrame.width
        val resizeHeight = height != null && height != imageFrame.height

        if (!resizeWidth && !resizeHeight && speed == 1.0) {
            return imageFrame
        }

        val newArgb: IntArray
        val newWidth: Int
        val newHeight: Int
        if (resizeWidth || resizeHeight) {
            val image = imageFrame.toBufferedImage()
            val resized = if (width != null && height != null) {
                ImmutableImage.wrapAwt(image)
                    .max(width, height)
                    .awt()
            } else if (width != null) {
                ImmutableImage.wrapAwt(image)
                    .scaleToWidth(width)
                    .awt()
            } else {
                ImmutableImage.wrapAwt(image)
                    .scaleToHeight(height!!)
                    .awt()
            }
            newArgb = resized.argb
            newWidth = resized.width
            newHeight = resized.height
        } else {
            newArgb = imageFrame.argb
            newWidth = imageFrame.width
            newHeight = imageFrame.height
        }

        return imageFrame.copy(
            argb = newArgb,
            width = newWidth,
            height = newHeight,
            duration = imageFrame.duration / speed,
        )
    }
}
