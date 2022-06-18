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
    val dest = BufferedImage(width, height, BufferedImage.TYPE_INT_RGB)

    // a matrix with the energy per node
    private val energy = Array(height) { Array(width) { 0.0 } }
    private var maxEnergy = 0.0

    // helper functions
    private fun bindX(x: Int, offset: Int = 0) = x.coerceIn(0 + offset, width - 1 - offset)
    private fun bindY(y: Int, offset: Int = 0) = y.coerceIn(0 + offset, height - 1 - offset)
    private fun color(x: Int, y: Int) = Color(src.getRGB(x, y))
    private fun pow2(i: Int) = i.toDouble().pow(2)
    private operator fun Color.minus(other: Color) =
        pow2(this.red - other.red) + pow2(this.green - other.green) + pow2(this.blue - other.blue)

    // calculate the energy for each pixel and fill the energy matrix
    private fun calculateEnergy() {
        for (x in 0 until width) {
            for (y in 0 until height) {
                // using bind-functions to coerce pixels on the border of the image to the inside of the border
                val dX = color(bindX(x, 1) - 1, y) - color(bindX(x, 1) + 1, y)
                val dY = color(x, bindY(y, 1) - 1) - color(x, bindY(y, 1) + 1)
                energy[y][x] = sqrt(dX + dY)

                maxEnergy = maxOf(energy[y][x], maxEnergy)
            }
        }
    }

    private fun transpose(matrix: Array<Array<Double>>): Array<Array<Double>> {
        val transposed = Array(energy[0].size) { Array(energy.size) { 0.0 } }
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

        // shadowing because I have to.. just to make it work for now
        val height = matrix.size
        val width = matrix[0].size
        fun bindX(x: Int, offset: Int = 0) = x.coerceIn(0 + offset, width - 1 - offset)
        fun bindY(y: Int, offset: Int = 0) = y.coerceIn(0 + offset, height - 1 - offset)

        // energy values for the first row are identical to the original values
        spv[0] = matrix[0]

        // for all other rows, for each pixel, new energy is equal to its own energy
        // plus the minimum of the three above, or two if we are at the horizontal border
        for (y in 1 until height) {
            for (x in 0 until width) {
//                println("x: $x; y: $y")
                spv[y][x] = matrix[y][x] + minOf(spv[y - 1][bindX(x - 1)], spv[y - 1][x], spv[y - 1][bindX(x + 1)])
            }
        }

        var x = spv[height - 1].indexOf(spv[height - 1].minOf { it })

        val seam = Array(height) { 0 }
        seam[height - 1] = x

        for (y in height - 2 downTo 0) {
            // using a map to preserve the indexes
            val map = mutableMapOf<Int, Double>(x to spv[y][x])
            if (x > 0) map[x - 1] = spv[y][x - 1]
            if (x < width - 1) map[x + 1] = spv[y][x + 1]

            seam[y] = map.minByOrNull { it.value }!!.key
            x = seam[y]
        }

        return seam
    }


    private fun writeImage(seam: Array<Int>) {
        for (x in 0 until width) {
            for (y in 0 until height) {
//                dest.setRGB(x, y, if (seam[y] == x) Color.RED.rgb else src.getRGB(x, y))
                dest.setRGB(x, y, if (seam[x] == y) Color.RED.rgb else src.getRGB(x, y))
            }
        }

        ImageIO.write(dest, "png", File(destName))
    }

    fun run() {
        calculateEnergy()


        writeImage(findHorizontalSeam())
    }
}

fun main(args: Array<String>) = SeamCarver(args[1], args[3]).run()
