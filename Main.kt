package seamcarving

import java.awt.Color
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO
import kotlin.math.pow
import kotlin.math.sqrt

class SeamCarver(srcName: String, private val destName: String) {
    // who needs error checking?
    private val src = ImageIO.read(File(srcName))
    private val width = src.width
    private val height = src.height
    private val dest = BufferedImage(width, height, BufferedImage.TYPE_INT_RGB)

    // a matrix with the energy per node
    private val energy = Array(height) { Array(width) { 0.0 } }
    private var maxEnergy = 0.0

    // calculate the energy for each pixel and fill the energy matrix
    private fun calculateEnergy() {
        fun color(x: Int, y: Int) = Color(src.getRGB(x, y))
        fun pow2(i: Int) = i.toDouble().pow(2)
        operator fun Color.minus(o: Color) = pow2(red - o.red) + pow2(green - o.green) + pow2(blue - o.blue)

        for (x in 0 until width) {
            for (y in 0 until height) {
                // coercing pixels on the border of the image to the inside of the border
                val dX = color(x.coerceIn(1, width - 2) - 1, y) - color(x.coerceIn(1, width - 2) + 1, y)
                val dY = color(x, y.coerceIn(1, height - 2) - 1) - color(x, y.coerceIn(1, height - 2) + 1)
                energy[y][x] = sqrt(dX + dY)

                maxEnergy = maxOf(energy[y][x], maxEnergy)
            }
        }
    }

    // this function can transpose any of our matrices (but not the src and dest images)
    private fun transpose(matrix: Array<Array<Double>>): Array<Array<Double>> {
        val transposed = Array(matrix[0].size) { Array(matrix.size) { 0.0 } }
        for (y in 0 until height) {
            for (x in 0 until width) {
                transposed[x][y] = matrix[y][x]
            }
        }
        return transposed
    }

    private fun findVerticalSeam() = findSeam(energy)
    private fun findHorizontalSeam() = findSeam(transpose(energy))
    private fun findSeam(matrix: Array<Array<Double>>): Array<Int> {
        // a matrix with the calculated Shortest Path Values
        val spv = Array(matrix.size) { Array(matrix[0].size) { 0.0 } }

        // shadowing because of transposing (for now)
        val height = matrix.size
        val width = matrix[0].size

        // energy values for the first row are identical to the original values
        spv[0] = matrix[0]

        // for all other rows, for each pixel, new energy is equal to its own energy
        // plus the minimum of the three above, or two if we are at the horizontal border
        for (y in 1 until height) {
            for (x in 0 until width) {
                spv[y][x] = matrix[y][x] +
                        minOf(
                            spv[y - 1][(x - 1).coerceAtLeast(0)],
                            spv[y - 1][x],
                            spv[y - 1][(x + 1).coerceAtMost(width - 1)]
                        )
            }
        }

        // find the path with the lowest energy
        var x = spv[height - 1].indexOf(spv[height - 1].minOf { it })

        // set up the seam from this point
        val seam = Array(height) { 0 }
        seam[height - 1] = x

        // travel up the path of the seam to y = 0
        for (y in height - 2 downTo 0) {
            // using a map to preserve the indexes
            val map = mutableMapOf(x to spv[y][x])
            if (x > 0) map[x - 1] = spv[y][x - 1]
            if (x < width - 1) map[x + 1] = spv[y][x + 1]

            seam[y] = map.minByOrNull { it.value }!!.key
            x = seam[y]
        }

        return seam
    }

    private fun writeImage(seam: Array<Int>, vertical: Boolean = true) {
        for (x in 0 until width) {
            for (y in 0 until height) {
                // check if this pixel is in the beam or not, and write it
                val inBeam = (vertical && seam[y] == x) || (!vertical && seam[x] == y)
                dest.setRGB(x, y, if (inBeam) Color.RED.rgb else src.getRGB(x, y))
            }
        }

        ImageIO.write(dest, "png", File(destName))
    }

    fun run() {
        calculateEnergy()
        writeImage(findHorizontalSeam(), false)
    }
}

fun main(args: Array<String>) = SeamCarver(args[1], args[3]).run()
