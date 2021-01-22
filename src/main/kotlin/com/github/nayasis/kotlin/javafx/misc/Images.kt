package com.github.nayasis.kotlin.javafx.misc

import com.github.nayasis.basica.base.Strings
import com.github.nayasis.basica.file.Files
import com.github.nayasis.basica.validation.Validator
import javafx.embed.swing.SwingFXUtils
import javafx.scene.image.Image
import javafx.scene.image.WritableImage
import javafx.scene.input.Clipboard
import javafx.scene.input.ClipboardContent
import javafx.scene.input.DragEvent
import javafx.scene.input.Dragboard
import javafx.scene.layout.BackgroundImage
import javafx.scene.layout.BackgroundPosition
import javafx.scene.layout.BackgroundRepeat
import javafx.scene.layout.BackgroundSize
import mu.KotlinLogging
import org.apache.http.client.methods.CloseableHttpResponse
import org.apache.http.client.methods.HttpGet
import org.apache.http.conn.ssl.NoopHostnameVerifier
import org.apache.http.conn.ssl.SSLConnectionSocketFactory
import org.apache.http.impl.client.CloseableHttpClient
import org.apache.http.impl.client.HttpClients
import org.apache.http.ssl.SSLContexts
import tornadofx.HttpClientEngine
import java.awt.AlphaComposite
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.io.UncheckedIOException
import java.net.MalformedURLException
import java.net.URL
import java.nio.file.Path
import java.security.KeyManagementException
import java.security.KeyStoreException
import java.security.NoSuchAlgorithmException
import java.security.cert.X509Certificate
import java.util.*
import javax.imageio.ImageIO
import javax.net.ssl.SSLContext
import javax.swing.ImageIcon
import javax.swing.filechooser.FileSystemView
import kotlin.collections.HashMap

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
        val stream = ByteArrayInputStream(binary)
        return try {
            ImageIO.read(stream)
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
    fun toJpgBinary(file: Path): ByteArray {
        return toJpgBinary(toImage(file))
    }

    fun toImage(event: DragEvent?): Image? {
        if (event == null) return null
        val dragboard = event.dragboard
        var image: Image? = null
        if (hasHtmlImgTag(dragboard)) {
            image = toImage(getSrcFromImageTag(dragboard.html))
        } else if (hasFile(dragboard)) {
            image = toImage(getRegularFile(dragboard.files))
        } else if (dragboard.hasUrl()) {
            image = toImage(dragboard.url)
        } else if (dragboard.hasString()) {
            image = toImage(dragboard.string)
        } else if (dragboard.hasImage()) {
            image = dragboard.image
        }
        return image
    }

    private fun getSrcFromImageTag(imgTag: String): String? {
        val info = imgTag.replaceFirst("(?i)^<img\\W.*?(src|srcset)=[\"|'](.*?)[\"|'].*?>".toRegex(),
            "$1 :: $2").split(" :: ".toRegex()).toTypedArray()
        val type = Strings.toLowerCase(info[0])
        val url = Strings.nvl(info[1]).replace("(?i)&amp;".toRegex(), "&")
        log.trace("html : {}\ntype : {}\nurl  : {}", imgTag, type, url)
        if ("srcset" == type) {
            val urls: MutableMap<String?, String> = HashMap()
            var tmpKey = 0
            for (line in Strings.split(url, ",")) {
                val values = Strings.split(line, " ")
                if (values.size == 2) {
                    urls[values[1]] = values[0]
                } else if (values.size == 1) {
                    urls[tmpKey++.toString()] = values[0]
                }
            }
            if (!urls.isEmpty()) {
                val keyset: List<String?> = ArrayList(urls.keys)
                Collections.reverse(keyset)
                return urls[keyset[0]]
            }
        } else if ("src" == type) {
            return url
        }
        return null
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
        val httpClient: CloseableHttpClient = getHttpClient()
        val request = HttpGet(url.toString())
        var response: CloseableHttpResponse? = null
        try {
            response = httpClient.execute(request)
            val entity: org.apache.http.HttpEntity = response.getEntity()
            if (entity != null) {
                entity.getContent().use { inputStream ->
                    val image: BufferedImage = ImageIO.read(inputStream)
                    return toImage(image)
                }
            } else {
                return null
            }
        } finally {
            try {
                response?.close()
            } catch (ignored: Exception) {}
            try {
                httpClient.close()
            } catch (ignored: Exception) {}
        }
    }

    private fun getHttpClient(): CloseableHttpClient {
        return try {
            val sslContext: SSLContext = SSLContexts.custom()
                .loadTrustMaterial(null, { _, _ -> true } )
                .build()
            val sslSocket = SSLConnectionSocketFactory(sslContext, NoopHostnameVerifier.INSTANCE)
            HttpClients.custom().setSSLSocketFactory(sslSocket).build()
        } catch (e: NoSuchAlgorithmException) {
            throw RuntimeException(e)
        } catch (e: KeyStoreException) {
            throw RuntimeException(e)
        } catch (e: KeyManagementException) {
            throw RuntimeException(e)
        }
    }

    fun toImage(path: Path): Image? {
        return if (Files.notExists(path)) null else toImage(path.toFile())
    }

    fun toImage(file: File?): Image? {
        return if (Files.notExists(file)) null else Image(file!!.toURI().toString())
    }

    fun toIconImage(file: File?): Image? {
        if (Files.notExists(file)) return null
        val readIcon = FileSystemView.getFileSystemView().getSystemIcon(file) as ImageIcon
        return try {
            toImage(readIcon.image as BufferedImage)
        } catch (e: UncheckedIOException) {
            throw com.github.nayasis.basica.exception.unchecked.UncheckedIOException(e)
        } catch (e: ClassCastException) {
            throw com.github.nayasis.basica.exception.unchecked.UncheckedIOException(e)
        }
    }

    fun toImage(url: String?): Image? {
        if (url == null) return null
        if (Validator.isFound(url, "^http(s?)://")) {
            return toImage(toURL(url))
        } else if (Validator.isFound(url, "^data:.*?;base64,")) {
            val encodedText = url.replaceFirst("^data:.*?;base64,".toRegex(), "")
            val binary = Base64.getDecoder().decode(encodedText)
            return toImage(binary)
        } else if (Files.isFile(url)) {
            return toImage(File(url))
        }
        return null
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
        return "data:image/jpeg;base64," + Base64.getMimeEncoder().encodeToString(output.toByteArray()).replace("[\n\r]".toRegex(), "")
    }

    private fun toURL(uri: String): URL? {
        return try {
            URL(uri)
        } catch (e: MalformedURLException) {
            log.trace(e.message, e)
            null
        }
    }

    fun isAcceptable(event: DragEvent?): Boolean {
        if (event == null) return false
        val dragboard = event.dragboard
        if (hasHtmlImgTag(dragboard)) return true
        if (dragboard.hasImage()) return true
        if (dragboard.hasUrl()) return true
        return if (dragboard.hasString()) true else hasFile(dragboard)
    }

    private fun hasFile(dragboard: Dragboard): Boolean {
        return dragboard.hasFiles() && hasRegularFile(dragboard.files)
    }

    private fun hasHtmlImgTag(dragboard: Dragboard): Boolean {
        return dragboard.hasHtml() && Validator.isFound(dragboard.html, "(?i)^<img\\W")
    }

    fun toImage(clipboard: Clipboard?): Image? {
        if (clipboard == null) return null
        var image: Image? = null
        if (clipboard.hasImage()) {
            image = clipboard.image
            val bytes = toJpgBinary(image)
            image = toImage(bytes)
        } else if (clipboard.hasFiles()) {
            val file = getRegularFile(clipboard.files)
            image = toImage(file)
        } else if (clipboard.hasUrl()) {
            image = toImage(clipboard.url)
        } else if (clipboard.hasString()) {
            image = toImage(clipboard.string)
        }
        return image
    }


    private fun hasRegularFile(files: List<File>): Boolean {
        return getRegularFile(files) != null
    }

    private fun getRegularFile(files: List<File>): File? {
        if (Validator.isEmpty(files)) return null
        for (file in files) {
            if (!file.isDirectory) return file
        }
        return null
    }

    fun isNotValid(image: Image?): Boolean {
        if (image == null) return true
        val width = image.width
        val height = image.height
        return width == 0.0 && height == 0.0
    }

    fun isValid(image: Image?): Boolean {
        return !isNotValid(image)
    }

    fun resize(image: BufferedImage?, width: Double, height: Double): BufferedImage? {
        if (image == null) return image
        val imgWidth = Math.round(width).toInt()
        val imgHeight = Math.round(height).toInt()
        val type = getType(image)
        val resizedImage = BufferedImage(imgWidth, imgHeight, type)
        val canvas = resizedImage.createGraphics()
        canvas.drawImage(image, 0, 0, imgWidth, imgHeight, null)
        canvas.dispose()
        return resizedImage
    }

    fun resize(image: BufferedImage?, maxPixel: Double): BufferedImage? {
        if (image == null) return image
        var width = Math.max(image.width, 1).toDouble()
        var height = Math.max(image.height, 1).toDouble()
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
        val originalImage = toBufferedImage(image)
        val resizedImage = resize(originalImage, maxPixel)
        return toJpgBinary(resizedImage)
    }

    fun rotate(image: Image?, angle: Double): Image? {
        if (image == null) return image
        val radian = Math.toRadians(angle)
        val sin = Math.abs(Math.sin(radian))
        val cos = Math.abs(Math.cos(radian))
        val width = image.width
        val height = image.height
        val canvasWidth = Math.floor(width * cos + height * sin).toInt()
        val canvasHeight = Math.floor(height * cos + width * sin).toInt()
        log.trace("rotate image\n\t- src : {} x {}\n\t- trg : {} x {}", width, height, canvasWidth, canvasHeight)
        val originalImage = toBufferedImage(image)
        val bufferedImage = BufferedImage(canvasWidth.toInt(), canvasHeight.toInt(), getType(originalImage))
        val canvas = bufferedImage.createGraphics()
        canvas.translate((canvasWidth - width) / 2.0, (canvasHeight - height) / 2.0)
        canvas.rotate(radian, width / 2, height / 2)
        canvas.drawRenderedImage(originalImage, null)
        canvas.dispose()
        return toImage(bufferedImage)
    }

    fun setImageToClipboard(image: Image?) {
        val clipboard = Clipboard.getSystemClipboard()
        val content = ClipboardContent()
        content.putImage(image)
        clipboard.setContent(content)
    }

    fun getImageFromClipboard(): Image? {
        val clipboard = Clipboard.getSystemClipboard()
        val contentTypes = clipboard.contentTypes
        for (format in contentTypes) {
            log.debug(format.toString())
        }
        log.trace("clipboard content types : {}", contentTypes)
        return toImage(clipboard)
    }

    fun toBackgroundImage(image: Image?): BackgroundImage? {
        val sizeProperty = BackgroundSize(BackgroundSize.AUTO, BackgroundSize.AUTO, true, true, true, false)
        return BackgroundImage(image,
            BackgroundRepeat.ROUND,
            BackgroundRepeat.ROUND,
            BackgroundPosition.CENTER,
            sizeProperty
        )
    }

    fun toBackgroundImage(uri: String?): BackgroundImage? {
        val image = Image(uri, 0.0, 0.0, false, true, true)
        return toBackgroundImage(image)
    }

    fun toFile(image: Image?, filePath: String?): File? {
        if (Validator.isEmpty(filePath)) return null
        Files.makeDir(Files.directory(filePath))
        var bufferedImage = toBufferedImage(image)
        val fileExtention = Validator.nvl(Files.extension(filePath), "jpg")
        val outputFile = File(filePath)
        if ("jpg".equals(fileExtention,true)) {
            bufferedImage = toBufferedImage(toJpgBinary(bufferedImage))
        }
        ImageIO.write(bufferedImage, fileExtention, outputFile)
        return outputFile
    }

}
