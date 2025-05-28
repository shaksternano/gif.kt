package com.shakster.gifkt.internal

/* NeuQuant Neural-Net Quantization Algorithm
 * ------------------------------------------
 *
 * Copyright (c) 1994 Anthony Dekker
 *
 * NEUQUANT Neural-Net quantization algorithm by Anthony Dekker, 1994.
 * See "Kohonen neural networks for optimal colour quantization"
 * in "Network: Computation in Neural Systems" Vol. 5 (1994) pp 351-367.
 * for a discussion of the algorithm.
 *
 * Any party obtaining a copy of these files from the author, directly or
 * indirectly, is granted, free of charge, a full and unrestricted irrevocable,
 * world-wide, paid up, royalty-free, nonexclusive right and license to deal
 * in this software and documentation files (the "Software"), including without
 * limitation the rights to use, copy, modify, merge, publish, distribute, sublicense,
 * and/or sell copies of the Software, and to permit persons who receive
 * copies from any such party to do so, with the only requirement being
 * that this copyright notice remain intact.
 */

internal class NeuQuant(
    /**
     * The image RGB data.
     */
    private val image: ByteArray,
    /**
     * Number of colors used.
     */
    private val maxColors: Int,
    /**
     * Sampling factor 1..30.
     */
    samplingFactor: Int,
) {

    // Constants
    companion object {
        /*
         * Four primes near 500 - assume no image has a length so large
         * that it is divisible by all four primes.
         */
        private const val PRIME_1: Int = 499
        private const val PRIME_2: Int = 491
        private const val PRIME_3: Int = 487
        private const val PRIME_4: Int = 503

        /**
         * Minimum size for input image.
         */
        private const val MIN_IMAGE_BYTES: Int = 3 * PRIME_4

        /* Program Skeleton
         * ----------------
         * [select samplingFactor in range 1..30]
         * [read image from input file]
         * image = (unsigned char*) malloc(3 * width * height);
         * initNet(image, 3 * width * height, samplingFactor);
         * learn();
         * unbiasNet();
         * [write output image header, using writeColorMap(f)]
         * inxBuild();
         * write output image using inxSearch(red, green, blue)
         */

        // Network Definitions
        /**
         * Bias for color values.
         */
        private const val NETWORK_BIAS_SHIFT: Int = 4

        /**
         * Number of learning cycles.
         */
        private const val CYCLES: Int = 100

        // Definitions for frequency and bias.
        /**
         * Bias for fractions.
         */
        private const val INT_BIAS_SHIFT: Int = 16
        private const val INT_BIAS: Int = 1 shl INT_BIAS_SHIFT

        /**
         * GAMMA = 1024
         */
        private const val GAMMA_SHIFT: Int = 10
        private const val BETA_SHIFT: Int = 10

        /**
         * BETA = 1/1024
         */
        private const val BETA: Int = INT_BIAS shr BETA_SHIFT
        private const val BETA_GAMMA: Int = INT_BIAS shl (GAMMA_SHIFT - BETA_SHIFT)

        // Definitions for decreasing radius factor.
        /**
         * At 32 biased by 6 bits.
         */
        private const val RADIUS_BIAS_SHIFT: Int = 6
        private const val RADIUS_BIAS: Int = 1 shl RADIUS_BIAS_SHIFT

        /**
         * Factor of 1/30 each cycle.
         */
        private const val RADIUS_DEC: Int = 30

        // Definitions for decreasing alpha factor.
        /**
         * Alpha starts at 1.0.
         */
        private const val ALPHA_BIAS_SHIFT: Int = 10
        private const val INIT_ALPHA: Int = 1 shl ALPHA_BIAS_SHIFT

        // RAD_BIAS and ALPHA_RAD_BIAS used for radPower calculation
        private const val RAD_BIAS_SHIFT: Int = 8
        private const val RAD_BIAS: Int = 1 shl RAD_BIAS_SHIFT
        private const val ALPHA_RAD_BIAS_SHIFT: Int = ALPHA_BIAS_SHIFT + RAD_BIAS_SHIFT
        private const val ALPHA_RAD_BIAS: Int = 1 shl ALPHA_RAD_BIAS_SHIFT
    }

    /**
     * lengthCount = H * W * 3.
     */
    private val lengthCount: Int = image.size

    /**
     * Sampling factor 1..30.
     */
    private val samplingFactor: Int = if (lengthCount < MIN_IMAGE_BYTES) {
        1
    } else {
        samplingFactor
    }

    private val maxNetPos: Int = maxColors - 1

    /**
     * For 256 cols, radius starts.
     */
    private val initRad: Int = maxColors shr 3

    private val initRadius: Int = initRad * RADIUS_BIAS

    /**
     * Biased by 10 bits.
     */
    private val alphaDec: Int = 30 + (this.samplingFactor - 1) / 3

    // Types and Global Variables

    // Bias and frequency arrays for learning.
    private val bias: IntArray = IntArray(maxColors)
    private val frequency: IntArray = IntArray(maxColors)

    /**
     * The network itself.
     */
    private val network: Array<IntArray> = Array(maxColors) { i ->
        // Initialize network in range (0, 0, 0) to (255, 255, 255) and set parameters
        val p = IntArray(4)
        val initial = (i shl (NETWORK_BIAS_SHIFT + 8)) / maxColors
        p[0] = initial
        p[1] = initial
        p[2] = initial
        bias[i] = 0
        // 1 / maxColors
        frequency[i] = INT_BIAS / maxColors
        p
    }

    /**
     * For network lookup - really 256.
     */
    private val networkIndex: IntArray = IntArray(256)

    /**
     * Rad power for precomputation.
     */
    private val radPower: IntArray = IntArray(initRad)

    private fun colorMap(): ByteArray {
        val map = ByteArray(3 * maxColors)
        val index = IntArray(maxColors)
        network.forEachIndexed { i, n ->
            index[n[3]] = i
        }
        var k = 0
        index.forEach { i ->
            map[k++] = network[i][2].toByte() // Red
            map[k++] = network[i][1].toByte() // Green
            map[k++] = network[i][0].toByte() // Blue
        }
        return map
    }

    /**
     * Insertion sort of network and building of networkIndex{0..255} (to do after unbias).
     */
    private fun inxBuild() {
        var previousCol = 0
        var startPos = 0
        network.forEachIndexed { i, p ->
            var smallPos = i
            // Index on green
            var smallVal = p[1]
            /* Find smallest in i..maxColors - 1 */
            var j = i + 1
            while (j < maxColors) {
                val q = network[j]
                // Index on green
                if (q[1] < smallVal) {
                    smallPos = j
                    // Index on green
                    smallVal = q[1]
                }
                j++
            }
            val q = network[smallPos]
            // Swap p[i] and q[smallPos] entries
            if (i != smallPos) {
                j = q[0]
                q[0] = p[0]
                p[0] = j
                j = q[1]
                q[1] = p[1]
                p[1] = j
                j = q[2]
                q[2] = p[2]
                p[2] = j
                j = q[3]
                q[3] = p[3]
                p[3] = j
            }
            // smallVal entry is now in position i
            if (smallVal != previousCol) {
                networkIndex[previousCol] = (startPos + i) shr 1
                j = previousCol + 1
                while (j < smallVal) {
                    networkIndex[j] = i
                    j++
                }
                previousCol = smallVal
                startPos = i
            }
        }
        networkIndex[previousCol] = (startPos + maxNetPos) shr 1
        var j = previousCol + 1
        // Really 256
        while (j < 256) {
            networkIndex[j] = maxNetPos
            j++
        }
    }

    /**
     * Main learning loop.
     */
    private fun learn() {
        var alpha = INIT_ALPHA
        var radius = initRadius
        var rad = radius shr RADIUS_BIAS_SHIFT
        for (i in 0..<rad) {
            radPower[i] = alpha * (((rad * rad - i * i) * RAD_BIAS) / (rad * rad))
        }

        val step = when {
            lengthCount < MIN_IMAGE_BYTES -> 3
            (lengthCount % PRIME_1) != 0 -> 3 * PRIME_1
            (lengthCount % PRIME_2) != 0 -> 3 * PRIME_2
            (lengthCount % PRIME_3) != 0 -> 3 * PRIME_3
            else -> 3 * PRIME_4
        }

        var pix = 0
        val samplePixels = lengthCount / (3 * samplingFactor)
        var delta = samplePixels / CYCLES
        for (i in 0..<samplePixels) {
            val red = (image[pix].toInt() and 0xFF) shl NETWORK_BIAS_SHIFT
            val green = (image[pix + 1].toInt() and 0xFF) shl NETWORK_BIAS_SHIFT
            val blue = (image[pix + 2].toInt() and 0xFF) shl NETWORK_BIAS_SHIFT
            val j = contest(red, green, blue)

            alterSingle(alpha, j, red, green, blue)
            if (rad != 0) {
                alterNeighbours(rad, j, red, green, blue)
            }

            pix += step
            if (pix >= lengthCount) {
                pix -= lengthCount
            }

            if (delta == 0) {
                delta = 1
            }
            if ((i + 1) % delta == 0) {
                alpha -= alpha / alphaDec
                radius -= radius / RADIUS_DEC
                rad = radius shr RADIUS_BIAS_SHIFT
                if (rad <= 1) {
                    rad = 0
                }
                for (k in 0..<rad) {
                    radPower[k] = alpha * (((rad * rad - k * k) * RAD_BIAS) / (rad * rad))
                }
            }
        }
    }

    /**
     * Search for RGB values 0..255 (after network is unbiased) and return color index.
     */
    fun map(red: Int, green: Int, blue: Int): Int {
        // Biggest possible dist is 256 * 3
        var bestD = 1000
        var best = -1
        // Index on green
        var i = networkIndex[green]
        // Start at networkIndex[green] and work outwards
        var j = i - 1
        while ((i < maxColors) || (j >= 0)) {
            if (i < maxColors) {
                val p = network[i]
                // inx key
                var dist = p[1] - green
                if (dist >= bestD) {
                    // Stop iterating
                    i = maxColors
                } else {
                    i++
                    if (dist < 0) {
                        dist = -dist
                    }
                    var a = p[0] - blue
                    if (a < 0) {
                        a = -a
                    }
                    dist += a
                    if (dist < bestD) {
                        var b = p[2] - red
                        if (b < 0) {
                            b = -b
                        }
                        dist += b
                        if (dist < bestD) {
                            bestD = dist
                            best = p[3]
                        }
                    }
                }
            }

            if (j >= 0) {
                val p = network[j]
                // inx key - reverse dif
                var dist = green - p[1]
                if (dist >= bestD) {
                    // Stop iterating
                    j = -1
                } else {
                    j--
                    if (dist < 0) {
                        dist = -dist
                    }
                    var a = p[0] - blue
                    if (a < 0) {
                        a = -a
                    }
                    dist += a
                    if (dist < bestD) {
                        var b = p[2] - red
                        if (b < 0) {
                            b = -b
                        }
                        dist += b
                        if (dist < bestD) {
                            bestD = dist
                            best = p[3]
                        }
                    }
                }
            }
        }
        return best
    }

    fun process(): ByteArray {
        learn()
        unbiasNet()
        inxBuild()
        return colorMap()
    }

    /**
     * Unbias network to give byte values 0..255 and record position i to prepare for sort.
     */
    private fun unbiasNet() {
        network.forEachIndexed { i, n ->
            n[0] = n[0] shr NETWORK_BIAS_SHIFT
            n[1] = n[1] shr NETWORK_BIAS_SHIFT
            n[2] = n[2] shr NETWORK_BIAS_SHIFT
            // Record color number
            network[i][3] = i
        }
    }

    /**
     * Move adjacent neurons by precomputed alpha * (1 - ((i - j)^2 / r^2)) in radPower[|i - j|].
     */
    private fun alterNeighbours(rad: Int, i: Int, red: Int, green: Int, blue: Int) {
        var low = i - rad
        if (low < -1) {
            low = -1
        }
        var high = i + rad
        if (high > maxColors) {
            high = maxColors
        }

        var j = i + 1
        var k = i - 1
        var m = 1
        while ((j < high) || (k > low)) {
            val a = radPower[m++]
            if (j < high) {
                val p = network[j++]
                p[0] -= (a * (p[0] - blue)) / ALPHA_RAD_BIAS
                p[1] -= (a * (p[1] - green)) / ALPHA_RAD_BIAS
                p[2] -= (a * (p[2] - red)) / ALPHA_RAD_BIAS
            }
            if (k > low) {
                val p = network[k--]
                p[0] -= (a * (p[0] - blue)) / ALPHA_RAD_BIAS
                p[1] -= (a * (p[1] - green)) / ALPHA_RAD_BIAS
                p[2] -= (a * (p[2] - red)) / ALPHA_RAD_BIAS
            }
        }
    }

    /**
     * Move neuron i towards biased (red, green, blue) by factor alpha.
     */
    private fun alterSingle(alpha: Int, i: Int, red: Int, green: Int, blue: Int) {
        // Alter hit neuron
        val n = network[i]
        n[0] -= (alpha * (n[0] - blue)) / INIT_ALPHA
        n[1] -= (alpha * (n[1] - green)) / INIT_ALPHA
        n[2] -= (alpha * (n[2] - red)) / INIT_ALPHA
    }

    /**
     * Search for biased RGB values.
     */
    private fun contest(red: Int, green: Int, blue: Int): Int {
        var bestD = (1 shl 31).inv()
        var bestBiasD = bestD
        var bestPos = -1
        var bestBiasPos = bestPos

        /*
         * Finds closest neuron (min dist) and updates frequency.
         * Finds the best neuron (min dist-bias) and returns position.
         * For frequently chosen neurons, frequency[i] is high and bias[i] is negative
         * bias[i] = gamma * ((1 / maxColors) - frequency[i])
         */
        network.forEachIndexed { i, n ->
            var dist = n[0] - blue
            if (dist < 0) {
                dist = -dist
            }
            var a = n[1] - green
            if (a < 0) {
                a = -a
            }
            dist += a
            a = n[2] - red
            if (a < 0) {
                a = -a
            }
            dist += a
            if (dist < bestD) {
                bestD = dist
                bestPos = i
            }
            val biasDist = dist - ((bias[i]) shr (INT_BIAS_SHIFT - NETWORK_BIAS_SHIFT))
            if (biasDist < bestBiasD) {
                bestBiasD = biasDist
                bestBiasPos = i
            }
            val betaFrequency = frequency[i] shr BETA_SHIFT
            frequency[i] -= betaFrequency
            bias[i] += betaFrequency shl GAMMA_SHIFT
        }
        frequency[bestPos] += BETA
        bias[bestPos] -= BETA_GAMMA
        return bestBiasPos
    }
}
