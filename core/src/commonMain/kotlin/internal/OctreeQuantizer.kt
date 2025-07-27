package com.shakster.gifkt.internal

import com.shakster.gifkt.ColorQuantizer
import com.shakster.gifkt.ColorTable

// Taken from https://github.com/delimitry/octree_color_quantizer
internal data object OctreeQuantizer : ColorQuantizer {

    /**
     * Limits the number of levels in the tree.
     */
    private const val MAX_TREE_DEPTH: Int = 8

    override fun quantize(rgb: ByteArray, maxColors: Int): ColorTable {
        val colorTable = OctreeColorTable()
        for (i in rgb.indices step 3) {
            val red = rgb[i].toInt() and 0xFF
            val green = rgb[i + 1].toInt() and 0xFF
            val blue = rgb[i + 2].toInt() and 0xFF
            colorTable.addColor(red, green, blue)
        }
        colorTable.makePalette(maxColors)
        return colorTable
    }

    private class OctreeColorTable : ColorTable {

        override lateinit var colors: ByteArray
            private set

        private val levels: Array<MutableList<OctreeNode>> = Array(MAX_TREE_DEPTH) {
            mutableListOf()
        }

        private val root: OctreeNode = OctreeNode(0)

        override fun getColorIndex(red: Int, green: Int, blue: Int): Int {
            return root.getPaletteIndex(red, green, blue, 0)
        }

        private fun getLeaves(): List<OctreeNode> {
            return root.getLeafNodes()
        }

        private fun addNode(level: Int, node: OctreeNode) {
            levels[level].add(node)
        }

        fun addColor(red: Int, green: Int, blue: Int) {
            root.addColor(red, green, blue, 0)
        }

        fun makePalette(maxColors: Int) {
            val palette = ByteList()
            var paletteIndex = 0
            var leafCount = getLeaves().size
            /*
             * Reduce nodes.
             * Up to 8 leaves can be reduced here, and the palette will have
             * only 248 colors (in the worst case) instead of the expected 256 colors.
             */
            for (level in MAX_TREE_DEPTH - 1 downTo 0) {
                val level = levels[level]
                if (level.isEmpty()) continue
                for (node in level) {
                    leafCount -= node.removeLeaves()
                    if (leafCount <= maxColors) break
                }
                if (leafCount <= maxColors) break
                level.clear()
            }
            // Build the palette.
            for (node in getLeaves()) {
                if (paletteIndex >= maxColors) break
                if (node.isLeaf) {
                    val averageRed = node.red / node.pixelCount
                    val averageGreen = node.green / node.pixelCount
                    val averageBlue = node.blue / node.pixelCount
                    palette.add(averageRed.toByte())
                    palette.add(averageGreen.toByte())
                    palette.add(averageBlue.toByte())
                }
                node.paletteIndex = paletteIndex++
            }
            colors = palette.toByteArray()
        }

        override fun toString(): String {
            return "OctreeColorTable(colors=${colors.contentToString()})"
        }

        private inner class OctreeNode(
            level: Int,
        ) {

            var red: Int = 0
            var green: Int = 0
            var blue: Int = 0
            var pixelCount: Int = 0
            var paletteIndex: Int = 0
            inline val isLeaf: Boolean
                get() = pixelCount > 0
            val children: Array<OctreeNode?> = arrayOfNulls(8)

            init {
                if (level < MAX_TREE_DEPTH - 1) {
                    addNode(level, this)
                }
            }

            fun getLeafNodes(): List<OctreeNode> {
                return buildList {
                    for (child in children) {
                        if (child == null) continue
                        if (child.isLeaf) {
                            add(child)
                        } else {
                            addAll(child.getLeafNodes())
                        }
                    }
                }
            }

            /**
             * Add a color to the tree.
             */
            fun addColor(red: Int, green: Int, blue: Int, level: Int) {
                if (level >= MAX_TREE_DEPTH) {
                    this.red += red
                    this.green += green
                    this.blue += blue
                    pixelCount++
                    return
                }

                val index = getColorIndex(red, green, blue, level)
                val child = children[index] ?: OctreeNode(level).also {
                    children[index] = it
                }
                child.addColor(red, green, blue, level + 1)
            }

            /**
             * Get the palette index for a color.
             * Uses [level] to go one level deeper if the node is not a leaf.
             */
            fun getPaletteIndex(red: Int, green: Int, blue: Int, level: Int): Int {
                if (isLeaf) {
                    return paletteIndex
                }
                val index = getColorIndex(red, green, blue, level)
                val child = children[index]
                if (child != null) {
                    return child.getPaletteIndex(red, green, blue, level + 1)
                }
                for (child in children) {
                    if (child == null) continue
                    return child.getPaletteIndex(red, green, blue, level + 1)
                }
                throw IllegalStateException("No child node found for color $red, $green, $blue at level $level")
            }

            /**
             * Add all children pixels count and color channels to parent node.
             * Return the number of removed leaves.
             */
            fun removeLeaves(): Int {
                var result = 0
                for (child in children) {
                    if (child == null) continue
                    red += child.red
                    green += child.green
                    blue += child.blue
                    pixelCount += child.pixelCount
                    result++
                }
                return result - 1
            }

            private fun getColorIndex(red: Int, green: Int, blue: Int, level: Int): Int {
                var index = 0
                val mask = 0x80 ushr level
                if (red and mask != 0) {
                    index = index or 4
                }
                if (green and mask != 0) {
                    index = index or 2
                }
                if (blue and mask != 0) {
                    index = index or 1
                }
                return index
            }

            override fun toString(): String {
                return "OctreeNode(" +
                    "red=$red" +
                    ", green=$green" +
                    ", blue=$blue" +
                    ", pixelCount=$pixelCount" +
                    ", paletteIndex=$paletteIndex" +
                    ", isLeaf=$isLeaf" +
                    ", children=${children.contentToString()}" +
                    ")"
            }
        }
    }
}
