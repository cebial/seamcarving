package seamcarving

import java.awt.Color
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO
import kotlin.math.pow
import kotlin.math.sqrt

fun main(args: Array<String>) {
    // get filenames
    val (_, srcName, _, destName) = args

    // read input file
    val src = ImageIO.read(File(srcName))

    // create canvas
    val dest = BufferedImage(src.width, src.height, BufferedImage.TYPE_INT_RGB)

    val energy = Array(src.height) { Array<Double>(src.width) { 0.0 } }

    // invert all the pixels
    for (x in 0 until src.width) {
        for (y in 0 until src.height) {
            val xBefore = Color(src.getRGB(x.coerceIn(1, src.width - 2) - 1, y))
            val xAfter = Color(src.getRGB(x.coerceIn(1, src.width - 2) + 1, y))

            val xRed = (xBefore.red - xAfter.red).toDouble().pow(2)
            val xGreen = (xBefore.green - xAfter.green).toDouble().pow(2)
            val xBlue = (xBefore.blue - xAfter.blue).toDouble().pow(2)

            val xEnergy = xRed + xGreen + xBlue

            val yBefore = Color(src.getRGB(x, y.coerceIn(1, src.height - 2) - 1))
            val yAfter = Color(src.getRGB(x, y.coerceIn(1, src.height - 2) + 1))

            val yRed = (yBefore.red - yAfter.red).toDouble().pow(2)
            val yGreen = (yBefore.green - yAfter.green).toDouble().pow(2)
            val yBlue = (yBefore.blue - yAfter.blue).toDouble().pow(2)

            val yEnergy = yRed + yGreen + yBlue

            val totalEnergy = xEnergy + yEnergy

            energy[y][x] = sqrt(totalEnergy)
        }
    }

    val maxEnergy = energy.flatten().maxOrNull() ?: 0.0

    for (x in 0 until src.width) {
        for (y in 0 until src.height) {
            val normalized = (255.0 * energy[y][x] / maxEnergy).toInt()
            val color = Color(normalized, normalized, normalized)
            dest.setRGB(x, y, color.rgb)
        }
    }

    // write the final image to disk
    ImageIO.write(dest, "png", File(destName))
}