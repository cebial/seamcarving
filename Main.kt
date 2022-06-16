package seamcarving

import java.awt.Color
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO

fun main(args: Array<String>) {
    // get filenames
    val (_, srcName, _, destName) = args

    // read input file
    val src = ImageIO.read(File(srcName))

    // create canvas
    val dest = BufferedImage(src.width, src.height, BufferedImage.TYPE_INT_RGB)

    // invert all the pixels
    for (x in 0 until src.width) {
        for (y in 0 until src.height) {
// all these methods work
// 1          val old = Color(src.getRGB(x, y))
//            val new = Color(255 - old.red, 255 - old.green, 255 - old.blue)
//            dest.setRGB(x, y, new.rgb)

//  2         dest.setRGB(x, y, 0xFFFFFF - src.getRGB(x, y))

            dest.setRGB(x, y, src.getRGB(x, y).inv())
        }
    }

    // write the final image to disk
    ImageIO.write(dest, "png", File(destName))
}
