package com.github.nayasis.simplelauncher.view.terminal

import com.github.nayasis.kotlin.basica.etc.error
import com.jediterm.pty.PtyProcessTtyConnector
import com.jediterm.terminal.TtyConnector
import com.jediterm.terminal.ui.JediTermWidget
import com.jediterm.terminal.ui.TerminalActionPresentation
import com.jediterm.terminal.ui.settings.DefaultSettingsProvider
import com.pty4j.PtyProcessBuilder
import mu.KotlinLogging
import java.awt.Font
import java.awt.FontMetrics
import java.awt.event.KeyEvent
import java.awt.event.KeyEvent.SHIFT_DOWN_MASK
import java.awt.image.BufferedImage
import java.nio.charset.StandardCharsets
import javax.swing.JFrame
import javax.swing.KeyStroke
import javax.swing.SwingUtilities
import javax.swing.UIManager

private val logger = KotlinLogging.logger {}
fun main() {
    try {
        UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName())
    } catch (e: Exception) {
        logger.error(e)
    }
    SwingUtilities.invokeLater {
        BasicTerminalShellExample().createAndShowGUI()
    }
}

class BasicTerminalShellExample {

    private fun createTerminalWidget(): JediTermWidget {
        val setting = object: DefaultSettingsProvider() {
            override fun getTerminalFont(): Font {
                /**
                 * Consolas
                 * NanumGothicCoding
                 * NanumGothic
                 * malgun
                 */
                return Font("맑은 고딕", Font.PLAIN, terminalFontSize.toInt())
            }
            override fun getTerminalFontWidthRatio(): Double {
                return if(isMonospaced(terminalFont)) 1.0 else 0.7
            }
            override fun getPasteActionPresentation(): TerminalActionPresentation {
                return TerminalActionPresentation("Paste", KeyStroke.getKeyStroke(KeyEvent.VK_INSERT, SHIFT_DOWN_MASK))
            }
        }
        return JediTermWidget(80, 40, setting).apply {
            ttyConnector = createTtyConnector()
            start()
        }
    }

    private fun createTtyConnector(): TtyConnector {
        return try {
            val command = arrayOf("cmd.exe")
            val process = PtyProcessBuilder().setCommand(command).start()
            PtyProcessTtyConnector(process, StandardCharsets.UTF_8)
        } catch (e: Exception) {
            throw IllegalStateException(e)
        }
    }

    fun createAndShowGUI() {
        JFrame("Basic Terminal Shell Example").apply {
            defaultCloseOperation = JFrame.EXIT_ON_CLOSE
            contentPane = createTerminalWidget()
            pack()
            isVisible = true
        }
    }

    private fun isMonospaced(font: Font): Boolean {
        val img = BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB)
        val graphics = img.createGraphics().apply { this.font = font }
        try {
            return isMonospaced(graphics.fontMetrics)
        } finally {
            runCatching { img.flush() }
            runCatching { graphics.dispose() }
        }
    }

    private fun isMonospaced(fontMetrics: FontMetrics): Boolean {
        var isMonospaced = true
        var charWidth = -1
        for (codePoint in 0..127) {
            if (Character.isValidCodePoint(codePoint)) {
                val character = codePoint.toChar()
                if (Character.isLetterOrDigit(character)) {
                    val w = fontMetrics.charWidth(character)
                    if (charWidth != -1) {
                        if (w != charWidth) {
                            isMonospaced = false
                            break
                        }
                    } else {
                        charWidth = w
                    }
                }
            }
        }
        return isMonospaced
    }

}