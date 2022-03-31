package com.github.nayasis.simplelauncher.view.jediterm

import com.jediterm.terminal.Questioner
import com.jediterm.terminal.TtyConnector
import com.jediterm.terminal.ui.JediTermWidget
import com.jediterm.terminal.ui.settings.DefaultSettingsProvider
import java.io.IOException
import java.io.PipedReader
import java.io.PipedWriter
import javax.swing.JFrame
import javax.swing.SwingUtilities

object BasicTerminalExample {
    private const val ESC = 27.toChar()
    @Throws(IOException::class)
    private fun writeTerminalCommands(writer: PipedWriter) {
        writer.write(ESC.toString() + "%G")
        writer.write(ESC.toString() + "[31m")
        writer.write("정화수님\r\n")
        writer.write(ESC.toString() + "[32;43m")
        writer.write("World\r\n")
    }

    private fun createTerminalWidget(): JediTermWidget {
        val widget = JediTermWidget(80, 24, SettingProvider())
        val terminalWriter = PipedWriter()
        widget.setTtyConnector(ExampleTtyConnector(terminalWriter))
        widget.start()
        try {
            writeTerminalCommands(terminalWriter)
            terminalWriter.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return widget
    }

    private fun createAndShowGUI() {
        val frame = JFrame("Basic Terminal Example")
        frame.defaultCloseOperation = JFrame.EXIT_ON_CLOSE
        frame.contentPane = createTerminalWidget()
        frame.pack()
        frame.isVisible = true
    }

    @JvmStatic
    fun main(args: Array<String>) {
        // Create and show this application's GUI in the event-dispatching thread.
        SwingUtilities.invokeLater(BasicTerminalExample::createAndShowGUI)
    }

    private class ExampleTtyConnector(writer: PipedWriter): TtyConnector {
        private var myReader: PipedReader? = null
        override fun init(q: Questioner): Boolean {
            return true
        }

        override fun close() {}
        override fun getName(): String {
            return ""
        }

        @Throws(IOException::class)
        override fun read(buf: CharArray, offset: Int, length: Int): Int {
            return myReader!!.read(buf, offset, length)
        }

        override fun write(bytes: ByteArray) {}
        override fun isConnected(): Boolean {
            return true
        }

        override fun write(string: String) {}
        override fun waitFor(): Int {
            return 0
        }

        @Throws(IOException::class)
        fun ready(): Boolean {
            return myReader!!.ready()
        }

        init {
            myReader = try {
                PipedReader(writer)
            } catch (e: IOException) {
                throw RuntimeException(e)
            }
        }
    }
}
