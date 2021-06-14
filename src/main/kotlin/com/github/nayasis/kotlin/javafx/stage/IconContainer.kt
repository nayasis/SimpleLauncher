package com.github.nayasis.kotlin.javafx.stage

import com.github.nayasis.kotlin.basica.core.klass.Classes
import com.github.nayasis.kotlin.basica.core.string.toFile
import com.github.nayasis.kotlin.javafx.misc.Images
import javafx.scene.image.Image
import net.sf.image4j.codec.ico.ICODecoder
import java.awt.image.BufferedImage
import java.io.File
import java.nio.file.Path

class IconContainer {

    val icons = ArrayList<Image>()

    private val addFn: (BufferedImage) -> Unit = { bufferedImage -> Images.toImage(bufferedImage)?.let { icons.add(it) } }

    fun add(icon: Image) = icons.add(icon)

    fun add(resourcePath: String) {
        Classes.getResourceStream(resourcePath).use { instream ->
            if( resourcePath.toFile().extension == "ico" ) {
                ICODecoder.read(instream).forEach(addFn)
            } else {
                icons.add(Image(instream))
            }
        }
    }

    fun add(file: File) = ICODecoder.read(file).forEach(addFn)

    fun add(path: Path) = add(path.toFile())

    fun clear() = icons.clear()

    fun isEmpty(): Boolean = icons.isEmpty()

}