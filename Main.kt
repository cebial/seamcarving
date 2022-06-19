package seamcarving

import java.awt.Color
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO
import kotlin.math.pow
import kotlin.math.sqrt

class SeamCarver(sourceName: String) {
    private val sourceFile = ImageIO.read(File(sourceName))
    private var width = sourceFile.width
    private var height = sourceFile.height
    private var src = MutableList(height) { mutableListOf<Color>() }
    private var isTransposed = false

    init {
        // fill an array with Color()s based on pixels, not worrying about memory/performance for now
        for (y in 0 until height)
            for (x in 0 until width)
                src[y] += Color(sourceFile.getRGB(x, y))
    }

    // calculate the energy for each pixel and return the energy matrix
    private fun calculateEnergy(): MutableList<MutableList<Double>> {
        fun pow2(i: Int) = i.toDouble().pow(2)
        operator fun Color.minus(o: Color) = pow2(red - o.red) + pow2(green - o.green) + pow2(blue - o.blue)

        val energy = MutableList(height) { mutableListOf<Double>() }

        for (y in 0 until height) {
            for (x in 0 until width) {
                // coercing pixels on the border of the image to the inside of the border
                val dX = src[y][x.coerceIn(1, width - 2) - 1] - src[y][x.coerceIn(1, width - 2) + 1]
                val dY = src[y.coerceIn(1, height - 2) - 1][x] - src[y.coerceIn(1, height - 2) + 1][x]
                energy[y] += sqrt(dX + dY)
            }
        }
        return energy
    }

    // transposing the array back and forth so we can use the same algorithm for both horizontal and vertical seams
    private fun transpose() {
        val newSrc = MutableList(src[0].size) { MutableList(src.size) { Color(0, 0, 0) } }

        for (y in 0 until height)
            for (x in 0 until width)
                newSrc[x][y] = src[y][x]

        src = newSrc
        height = src.size
        width = src[0].size
        isTransposed = !isTransposed
    }

    fun removeVerticalSeam() = removeSeam()
    fun removeHorizontalSeam() {
        if (!isTransposed) transpose()
        removeSeam()
    }

    private fun removeSeam() {
        val energy = calculateEnergy()

        // calculate the Shortest Path Values
        val spv = MutableList(energy.size) { MutableList(energy[0].size) { 0.0 } }

        // energy values for the first row are identical to the original values
        spv[0] = energy[0]

        // for all other rows, for each pixel, new energy is equal to its own energy
        // plus the minimum of the three above, or two if we are at the horizontal border
        for (y in 1 until height)
            for (x in 0 until width)
                spv[y][x] = energy[y][x] +
                        minOf(
                            spv[y - 1][(x - 1).coerceAtLeast(0)],
                            spv[y - 1][x],
                            spv[y - 1][(x + 1).coerceAtMost(width - 1)]
                        )

        // find the path with the lowest energy
        var x = spv[height - 1].indexOf(spv[height - 1].minOf { it })

        // remove the bottom pixel of the seam
        src[height - 1].removeAt(x)

        // travel up the lowest energy path to y = 0
        for (y in height - 2 downTo 0) {
            // using a map to preserve the indexes
            val map = mutableMapOf(x to spv[y][x])
            if (x > 0) map[x - 1] = spv[y][x - 1]
            if (x < width - 1) map[x + 1] = spv[y][x + 1]
            x = map.minByOrNull { it.value }!!.key

            // remove the pixel we found in this row
            src[y].removeAt(x)
        }

        width--
    }

    fun writeImage(destName: String) {
        if (isTransposed) transpose()
        val dest = BufferedImage(width, height, BufferedImage.TYPE_INT_RGB)

        for (x in 0 until width)
            for (y in 0 until height)
                dest.setRGB(x, y, src[y][x].rgb)

        ImageIO.write(dest, "png", File(destName))
    }
}

fun main(args: Array<String>) {
    val carver = SeamCarver(args[1])

    repeat(args[5].toInt()) { carver.removeVerticalSeam() }
    repeat(args[7].toInt()) { carver.removeHorizontalSeam() }

    carver.writeImage(args[3])
}
