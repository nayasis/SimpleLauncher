package com.github.nayasis.kotlin.javafx.misc

import com.github.nayasis.kotlin.basica.etc.Platforms
import javafx.scene.image.Image
import javafx.scene.input.Clipboard
import javafx.scene.input.ClipboardContent
import org.apache.commons.exec.CommandLine
import org.apache.commons.exec.DefaultExecutor
import java.awt.Font
import java.awt.GraphicsEnvironment
import java.awt.Toolkit
import java.io.File
import java.net.URI
import java.awt.Desktop as AwtDesktop

object Desktop {

    init {
        if (Platforms.isMac)
            System.setProperty("javafx.macosx.embedded", "true")
    }

    fun toolkit(): Toolkit {
        return Toolkit.getDefaultToolkit()
    }

    private fun clipboard(): Clipboard {
        return Clipboard.getSystemClipboard()
    }

    private fun support(action: AwtDesktop.Action): Boolean {
        return when {
            !AwtDesktop.isDesktopSupported() -> false
            else -> AwtDesktop.getDesktop().isSupported(action)
        }
    }

    private fun execute(command: String, parameter: String? = null): Boolean {
        return try {
            val commandline = CommandLine(command)
            if( ! parameter.isNullOrEmpty() )
                commandline.addArgument(parameter)
            DefaultExecutor().execute(commandline)
            true
        } catch (e: Exception) {
            false
        }
    }

    private fun openOsSpecific(what: String?) {
        if (Platforms.isLinux || Platforms.isUnix || Platforms.isSolaris) {
            if (execute("kde-open",   what)) return
            if (execute("gnome-open", what)) return
            if (execute("xdg-open",   what)) return
            throw IllegalArgumentException("fail to open(kde, gnome, xdg).")
        } else if (Platforms.isMac) {
            execute("open", what)
        } else if (Platforms.isWindows) {
            execute("explorer", what)
        }
    }

    fun browse(uri: String) {
        if (support(AwtDesktop.Action.BROWSE)) {
            AwtDesktop.getDesktop().browse(URI(uri))
        } else {
            openOsSpecific(uri)
        }
    }

    fun open(file: File) {
        if (support(AwtDesktop.Action.OPEN)) {
            AwtDesktop.getDesktop().open(file)
        } else {
            openOsSpecific(file.path)
        }
    }

    fun edit(file: File) {
        if (support(AwtDesktop.Action.EDIT)) {
            AwtDesktop.getDesktop().edit(file)
        } else {
            openOsSpecific(file.path)
        }
    }

    fun graphics(): GraphicsEnvironment = GraphicsEnvironment.getLocalGraphicsEnvironment()

    fun allFonts(): List<Font> = graphics().allFonts.toList()

}

private fun clipboard(): Clipboard = Clipboard.getSystemClipboard()

fun Clipboard.set(text: String?) {
    text?.let{ clipboard().setContent(ClipboardContent().apply {putString(it)}) }
}

fun Clipboard.set(image: Image?) {
    image?.let { clipboard().setContent(ClipboardContent().apply {putImage(it)}) }
}

fun Clipboard.getText(): String {
    return clipboard().string
}

fun Clipboard.getImage(): Image? {
    return Images.toImage(clipboard())
}