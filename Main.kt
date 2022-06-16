package seamcarving

import java.awt.Color
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.sqrt

class SeamCarver(srcName: String, private val destName: String) {
    private val src = ImageIO.read(File(srcName))
    private val energy = Array(src.height) { Array(src.width) { 0.0 } }
    private var maxEnergy = 0.0

    private fun calculateEnergy() {
        val xMax = src.width - 2
        val yMax = src.height - 2

        for (x in 0 until src.width) {
            for (y in 0 until src.height) {
                val dX = color(x.coerceIn(1, xMax) - 1, y) - color(x.coerceIn(1, xMax) + 1, y)
                val dY = color(x, y.coerceIn(1, yMax) - 1) - color(x, y.coerceIn(1, yMax) + 1)
                energy[y][x] = sqrt(dX + dY)

                maxEnergy = max(energy[y][x], maxEnergy)
            }
        }
    }

    private fun writeImage() {
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
        writeImage()
    }
}

fun main(args: Array<String>) = SeamCarver(args[1], args[3]).run()
