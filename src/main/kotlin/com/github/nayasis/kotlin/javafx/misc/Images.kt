@file:Suppress("MemberVisibilityCanBePrivate")

package com.github.nayasis.kotlin.javafx.misc

import com.github.nayasis.basica.file.Files
import com.github.nayasis.kotlin.basica.extension
import com.github.nayasis.kotlin.basica.isFile
import com.github.nayasis.kotlin.basica.decodeBase64
import com.github.nayasis.kotlin.basica.encodeBase64
import com.github.nayasis.kotlin.basica.found
import com.github.nayasis.kotlin.basica.toDir
import com.github.nayasis.kotlin.basica.toFile
import com.github.nayasis.kotlin.basica.toUrl
import javafx.embed.swing.SwingFXUtils
import javafx.scene.image.Image
import javafx.scene.image.WritableImage
import javafx.scene.input.Clipboard
import javafx.scene.input.DragEvent
import javafx.scene.input.Dragboard
import javafx.scene.layout.BackgroundImage
import javafx.scene.layout.BackgroundPosition
import javafx.scene.layout.BackgroundRepeat
import javafx.scene.layout.BackgroundSize
import mu.KotlinLogging
import org.apache.http.HttpEntity
import org.apache.http.client.methods.CloseableHttpResponse
import org.apache.http.client.methods.HttpGet
import org.apache.http.conn.ssl.NoopHostnameVerifier
import org.apache.http.conn.ssl.SSLConnectionSocketFactory
import org.apache.http.impl.client.CloseableHttpClient
import org.apache.http.impl.client.HttpClients
import org.apache.http.ssl.SSLContexts
import java.awt.AlphaComposite
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.lang.Math.round
import java.lang.Math.toRadians
import java.net.URL
import java.nio.file.Path
import java.util.*
import javax.imageio.ImageIO
import javax.net.ssl.SSLContext
import javax.swing.ImageIcon
import javax.swing.filechooser.FileSystemView
import kotlin.collections.HashMap
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.roundToInt
import kotlin.math.sin

private val log = KotlinLogging.logger {}

object Images {

    /**
     * convert image to BufferedImage
     * @param image javafx image
     * @return BufferedImage
     */
    fun toBufferedImage(image: Image?): BufferedImage? {
        return if (image == null) null else SwingFXUtils.fromFXImage(image, null)
    }

    /**
     * Convert binary (image) data to BufferedImage
     *
     * @param binary binary image data
     * @return buffered image
     */
    fun toBufferedImage(binary: ByteArray?): BufferedImage? {
        if( binary == null ) return null
        return try {
            ImageIO.read(ByteArrayInputStream(binary))
        } catch (e: Exception) {
            log.error(e.message, e)
            null
        }
    }

    /**
     * get image binary data
     *
     * @param image     image
     * @param format    image format (jpg, png, ... )
     * @return binary data
     */
    fun toBinary(image: BufferedImage?, format: String?): ByteArray {
        return try {
            val stream = ByteArrayOutputStream()
            ImageIO.write(image, format, stream)
            stream.toByteArray()
        } catch (e: Exception) {
            byteArrayOf()
        }
    }

    /**
     * get image binary data
     *
     * @param image     image
     * @param format    image format (jpg, png, ... )
     * @return binary data
     */
    fun toBinary(image: Image?, format: String?): ByteArray {
        return try {
            val stream = ByteArrayOutputStream()
            ImageIO.write(toBufferedImage(image), format, stream)
            stream.toByteArray()
        } catch (e: Exception) {
            byteArrayOf()
        }
    }

    /**
     * get JPG format binary data
     *
     * @param rawImage image
     * @return binary data
     */
    fun toJpgBinary(rawImage: BufferedImage?): ByteArray {
        return try {
            val rgbImage = BufferedImage(rawImage!!.width, rawImage.height, BufferedImage.TYPE_INT_RGB)
            val graphics = rgbImage.createGraphics()

            // remove png alpha channel
            graphics.composite = AlphaComposite.Src

            // copy source image to target
            graphics.drawImage(rawImage, 0, 0, null)
            val stream = ByteArrayOutputStream()
            ImageIO.write(rgbImage, "jpg", stream)
            graphics.dispose()
            stream.toByteArray()
        } catch (e: Exception) {
            byteArrayOf()
        }
    }

    fun copy(image: Image): WritableImage {
        val height = image.height.toInt()
        val width = image.width.toInt()
        val pixelReader = image.pixelReader
        val writableImage = WritableImage(width, height)
        val pixelWriter = writableImage.pixelWriter
        for (y in 0 until height) {
            for (x in 0 until width) {
                val color = pixelReader.getColor(x, y)
                pixelWriter.setColor(x, y, color)
            }
        }
        return writableImage
    }

    /**
     * get JPG format binary data
     *
     * @param image image
     * @return binary data
     */
    fun toJpgBinary(image: Image?): ByteArray {
        return toJpgBinary(toBufferedImage(image))
    }

    /**
     * get JPG format binary data
     *
     * @param file image file
     * @return binary data
     */
    fun toJpgBinary(file: File?): ByteArray {
        return toJpgBinary(toImage(file))
    }

    /**
     * get JPG format binary data
     *
     * @param file image file
     * @return binary data
     */
    fun toJpgBinary(file: Path?): ByteArray {
        return toJpgBinary(toImage(file))
    }

    fun toImage(event: DragEvent?): Image? {
        if (event == null) return null
        val src = event.dragboard
        return when {
            src.hasHtmlImgTag() -> toImage(getSrcFromImageTag(src.html))
            src.hasRegularFile() -> toImage(getRegularFile(src.files))
            src.hasUrl() -> toImage(src.url)
            src.hasString() -> toImage(src.string)
            src.hasImage() -> src.image
            else -> null
        }
    }

    private fun getSrcFromImageTag(imgTag: String): String? {

        val regex = "(?is)^<img\\W.*?(src|srcset)=[\"|'](.*?)[\"|'].*?>".toRegex()
        val info = imgTag.replaceFirst(regex, "$1 :: $2").split(" :: ")
        val type = info[0].toLowerCase()
        val url  = info[1].replace("(?is)&amp;".toRegex(), "&")

        log.trace { "html : ${imgTag}\ntype : ${type}\nurl  : ${url}" }

        return when(type) {
            "src" -> url
            "srcset" -> {
                val urls = HashMap<String,String>()
                var tmpKey = 0
                for (line in url.split(",")) {
                    val values = line.split(" ")
                    if (values.size == 2) {
                        urls[values[1]] = values[0]
                    } else if (values.size == 1) {
                        urls[tmpKey++.toString()] = values[0]
                    }
                }
                if(urls.isNotEmpty()) urls[ ArrayList(urls.keys).apply{reverse()}[0] ] else null
            }
            else -> null
        }

    }

    fun toImage(image: BufferedImage?): Image? {
        return SwingFXUtils.toFXImage(image, null)
    }

    fun toImage(binary: ByteArray?): Image? {
        val stream = ByteArrayInputStream(binary)
        val image = ImageIO.read(stream)
        return toImage(image)
    }

    fun toImage(url: URL?): Image? {

        val httpClient = getHttpClient()
        val request = HttpGet(url.toString())
        var response: CloseableHttpResponse? = null

        return try {
            response = httpClient.execute(request)
            val image: BufferedImage = ImageIO.read(response.getEntity().content)
            toImage(image)
        } catch (e: Exception) {
            log.error(e.message,e)
            null
        } finally {
            try { response?.close()  } catch (ignored: Exception) {}
            try { httpClient.close() } catch (ignored: Exception) {}
        }

    }

    private fun getHttpClient(): CloseableHttpClient {
        val sslContext: SSLContext = SSLContexts.custom()
            .loadTrustMaterial(null) { _, _ -> true }
            .build()
        val sslSocket = SSLConnectionSocketFactory(sslContext, NoopHostnameVerifier.INSTANCE)
        return HttpClients.custom().setSSLSocketFactory(sslSocket).build()
    }

    fun toImage(path: Path?): Image? {
        return when {
            path == null -> null
            path.isFile() -> toImage(path.toFile())
            else -> null
        }
    }

    fun toImage(file: File?): Image? {
        return when {
            file == null -> null
            file.isFile -> Image(file.toURI().toString())
            else -> null
        }
    }

    fun toIconImage(file: File?): Image? {
        if( file == null || ! file.isFile ) return null
        val icon = FileSystemView.getFileSystemView().getSystemIcon(file) as ImageIcon?
        return if( icon == null ) null else toImage(icon.image as BufferedImage)
    }

    fun toImage(url: String?): Image? {
        return when {
            url == null -> null
            url.found("^http(s?)://".toRegex()) -> toImage(url.toUrl())
            url.found("^data:.*?;base64,".toRegex()) -> {
                val encoded = url.replaceFirst("^data:.*?;base64,".toRegex(), "")
                toImage(encoded.decodeBase64())
            }
            url.toFile().exists() -> toImage(url.toFile())
            else -> null
        }
    }

    /**
     * get base64 encoded image text
     *
     * @param url URL, file path, encoded image text (^data:.*?;base64,)
     * @return formatted (data:image/jpeg;base64,****) text
     */
    fun toBase64Image(url: String?): String? {
        val output = ByteArrayOutputStream()
        var image = toBufferedImage(toImage(url))
        image = toBufferedImage(toJpgBinary(image))
        ImageIO.write(image, "jpg", output)
        return "data:image/jpeg;base64," + output.toByteArray().encodeBase64().replace("[\n\r]".toRegex(), "")
    }

    fun isAcceptable(event: DragEvent?): Boolean {
        with(event) {
            return when {
                this == null -> false
                dragboard.hasHtmlImgTag() -> true
                dragboard.hasImage() -> true
                dragboard.hasUrl() -> true
                dragboard.hasRegularFile() -> true
                dragboard.hasString() -> true
                else -> false
            }
        }
    }

    fun toImage(clipboard: Clipboard?): Image? {
        with(clipboard) {
            if (this == null) return null
            return when {
                hasImage()  -> toImage(toJpgBinary(image))
                hasFiles()  -> toImage(getRegularFile(files))
                hasUrl()    -> toImage(url)
                hasString() -> toImage(string)
                else -> null
            }
        }
    }

    fun resize(image: BufferedImage?, width: Double, height: Double): BufferedImage? {
        if (image == null) return image
        val imgWidth = width.roundToInt()
        val imgHeight = height.roundToInt()
        val type = getType(image)
        val resizedImage = BufferedImage(imgWidth, imgHeight, type)
        val canvas = resizedImage.createGraphics()
        canvas.drawImage(image, 0, 0, imgWidth, imgHeight, null)
        canvas.dispose()
        return resizedImage
    }

    fun resize(image: BufferedImage?, maxPixel: Double): BufferedImage? {
        if (image == null) return image
        var width = image.width.coerceAtLeast(1).toDouble()
        var height = image.height.coerceAtLeast(1).toDouble()
        if (width < maxPixel && height < maxPixel) return image
        if (width > height) {
            height = height * maxPixel / width
            width = maxPixel
        } else {
            width = width * maxPixel / height
            height = maxPixel
        }
        return resize(image, width, height)
    }

    private fun getType(image: BufferedImage?): Int {
        return if (image!!.type == 0) BufferedImage.TYPE_INT_ARGB else image.type
    }

    fun resize(image: Image?, maxPixel: Double): Image? {
        if (image == null) return image
        val originalImage = toBufferedImage(image)
        val resizedImage = resize(originalImage, maxPixel)
        return toImage(resizedImage)
    }

    fun resize(image: ByteArray?, maxPixel: Double): ByteArray? {
        if (image == null) return image
        val original = toBufferedImage(image)
        val resized = resize(original, maxPixel)
        return toJpgBinary(resized)
    }

    fun rotate(image: Image?, angle: Double): Image? {

        if (image == null) return image

        val radian = toRadians(angle)
        val sin = abs(sin(radian))
        val cos = abs(Math.cos(radian))
        val width = image.width
        val height = image.height
        val canvasWidth = floor(width * cos + height * sin).toInt()
        val canvasHeight = floor(height * cos + width * sin).toInt()

        log.trace { "rotate image\n\t- src : ${width} x ${height}\n\t- trg : ${canvasWidth} x ${canvasHeight}" }

        val originalImage = toBufferedImage(image)
        val bufferedImage = BufferedImage(canvasWidth.toInt(), canvasHeight.toInt(), getType(originalImage))

        with(bufferedImage.createGraphics()) {
            translate((canvasWidth - width) / 2.0, (canvasHeight - height) / 2.0)
            rotate(radian, width / 2, height / 2)
            drawRenderedImage(originalImage, null)
            dispose()
        }

        return toImage(bufferedImage)

    }

    fun toBackgroundImage(image: Image?): BackgroundImage? {
        if( image == null ) return image
        val sizeProperty = BackgroundSize(BackgroundSize.AUTO, BackgroundSize.AUTO, true, true, true, false)
        return BackgroundImage(image,
            BackgroundRepeat.ROUND,
            BackgroundRepeat.ROUND,
            BackgroundPosition.CENTER,
            sizeProperty
        )
    }

    fun toBackgroundImage(uri: String?): BackgroundImage? {
        return uri?.let {
            val image = Image(uri, 0.0, 0.0, false, true, true)
            toBackgroundImage(image)
        }
    }

    fun toFile(image: Image?, path: String?): File? {
        if( image == null || path.isNullOrEmpty() ) return null
        Files.makeDir(path.toDir())
        val output = path.toFile()
        val extension = output.extension("jpg")
        var bufferedImage = toBufferedImage(image)
        if ("jpg" == extension )
            bufferedImage = toBufferedImage(toJpgBinary(bufferedImage))
        ImageIO.write(bufferedImage, extension, output)
        return output
    }

}

private fun Dragboard.hasRegularFile(): Boolean {
    return this.hasFiles() && getRegularFile(this.files) != null
}

private fun Dragboard.hasHtmlImgTag(): Boolean {
    return this.hasHtml() && this.html.found("(?is)^<img\\W".toPattern())
}

private fun Image?.isValid(): Boolean {
    return (this?.width ?: 0.0) * (this?.height ?: 0.0) > 0
}

private fun getRegularFile(files: List<File>?): File? {
    return files?.firstOrNull { it.isFile } ?: null
}
