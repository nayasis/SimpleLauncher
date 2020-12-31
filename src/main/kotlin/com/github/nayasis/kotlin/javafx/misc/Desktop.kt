package com.github.nayasis.kotlin.javafx.misc

import com.github.nayasis.basica.cli.Command
import com.github.nayasis.basica.cli.CommandExecutor
import com.github.nayasis.basica.exception.unchecked.CommandLineException
import javafx.scene.image.Image
import javafx.scene.input.Clipboard
import javafx.scene.input.ClipboardContent
import java.awt.Font
import java.awt.GraphicsEnvironment
import java.awt.Toolkit
import java.io.File
import java.net.URI
import java.awt.Desktop as AwtDesktop

val Desktop = object {

    init {
        if (Platforms.isMac())
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

    private fun execute(command: String, parameter: String?): Boolean {
        return try {
            val executor = CommandExecutor()
            executor.run(Command().add(command).addQuote(parameter))
            true
        } catch (e: CommandLineException) {
            false
        }
    }

    private fun openOsSpecific(what: String?) {
        if (Platforms.isLinux() || Platforms.isUnix() || Platforms.isSolaris()) {
            if (execute("kde-open", what)) return
            if (execute("gnome-open", what)) return
            if (execute("xdg-open", what)) return
            throw CommandLineException("fail to open(kde, gnome, xdg).")
        } else if (Platforms.isMac()) {
            execute("open", what)
        } else if (Platforms.isWindows()) {
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

    fun getAllFonts(): List<Font> = graphics().allFonts.toList()

}

fun Clipboard.set(text: String?) {
    setContent(ClipboardContent().apply { putString(text) })
}

fun Clipboard.set(image: Image?) {
    image.let { setContent(ClipboardContent().apply { putImage(it) }) }
}
