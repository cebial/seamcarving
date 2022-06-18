package seamcarving

import java.awt.Color
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO
import kotlin.math.pow
import kotlin.math.sqrt

class SeamCarver(srcName: String, private val destName: String) {
    private val src = ImageIO.read(File(srcName))
    private val energy = Array(src.height) { Array(src.width) { 0.0 } }
    private val energyDP = Array(src.height) { Array(src.width) { 0.0 } }
    private var maxEnergy = 0.0
    private val currentSeam = Array(src.height) { 0 }

    private fun calculateEnergy() {
        val xMax = src.width - 2
        val yMax = src.height - 2

        for (x in 0 until src.width) {
            for (y in 0 until src.height) {
                val dX = color(x.coerceIn(1, xMax) - 1, y) - color(x.coerceIn(1, xMax) + 1, y)
                val dY = color(x, y.coerceIn(1, yMax) - 1) - color(x, y.coerceIn(1, yMax) + 1)
                energy[y][x] = sqrt(dX + dY)

                maxEnergy = maxOf(energy[y][x], maxEnergy)
            }
        }
    }

    private fun findVerticalSeam() {
        // energy values for the first row are identical to the original values
        energyDP[0] = energy[0]

        // for all other rows, for each pixel, new energy is equal to its own energy
        // plus the minimum of the three above
        for (y in 1 until src.height) {
            for (x in 0 until src.width) {
                energyDP[y][x] = energy[y][x] + when (x) {
                    0 -> minOf(energyDP[y - 1][x], energyDP[y - 1][x + 1])
                    src.width - 1 -> minOf(energyDP[y - 1][x - 1], energyDP[y - 1][x])
                    else -> minOf(energyDP[y - 1][x - 1], energyDP[y - 1][x], energyDP[y - 1][x + 1])
                }
            }
        }

        var x = energyDP[src.height - 1].indexOf(energyDP[src.height - 1].minOf { it })

        currentSeam[src.height - 1] = x

        for (y in src.height - 2 downTo 0) {
            val map = mutableMapOf<Int, Double>()
            if (x > 0) map[x - 1] = energyDP[y][x - 1]
            map[x] = energyDP[y][x]
            if (x < src.width - 1) map[x + 1] = energyDP[y][x + 1]

            currentSeam[y] = map.minByOrNull { it.value }!!.key
            x = currentSeam[y]
        }
        currentSeam.forEach(::println)

    }

    private fun writeImageWithSeam() {
        val dest = BufferedImage(src.width, src.height, BufferedImage.TYPE_INT_RGB)

        for (x in 0 until src.width) {
            for (y in 0 until src.height) {
                val col = if (currentSeam[y] == x) Color.RED else Color(src.getRGB(x, y))
                dest.setRGB(x, y, col.rgb)
            }
        }


        ImageIO.write(dest, "png", File(destName))
    }

    private fun writeImageWithIntensity() {
        val dest = BufferedImage(src.width, src.height, BufferedImage.TYPE_INT_RGB)

        for (x in 0 until src.width) {
            for (y in 0 until src.height) {
                val intensity = (255.0 * energy[y][x] / maxEnergy).toInt()
                dest.setRGB(x, y, Color(intensity, intensity, intensity).rgb)
            }
        }

        ImageIO.write(dest, "png", File(destName))
    }

    private fun color(x: Int, y: Int) = Color(src.getRGB(x, y))
    private fun pow2(i: Int) = i.toDouble().pow(2)
    private operator fun Color.minus(other: Color) =
        pow2(this.red - other.red) + pow2(this.green - other.green) + pow2(this.blue - other.blue)

    fun run() {
        calculateEnergy()
        findVerticalSeam()
        writeImageWithSeam()
    }
}

fun main(args: Array<String>) = SeamCarver(args[1], args[3]).run()
