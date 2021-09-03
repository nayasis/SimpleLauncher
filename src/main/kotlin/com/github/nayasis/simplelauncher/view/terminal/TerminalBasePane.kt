package com.github.nayasis.simplelauncher.view.terminal

import com.github.nayasis.kotlin.basica.core.klass.Classes
import com.github.nayasis.kotlin.basica.reflection.Reflector
import com.github.nayasis.kotlin.javafx.misc.Desktop
import com.github.nayasis.kotlin.javafx.misc.set
import javafx.beans.property.SimpleObjectProperty
import javafx.concurrent.Task
import javafx.scene.layout.Pane
import javafx.scene.web.WebView
import mu.KotlinLogging
import netscape.javascript.JSObject
import tornadofx.FXTask
import tornadofx.runAsync
import tornadofx.runLater
import java.io.BufferedReader
import java.io.Reader

private val logger = KotlinLogging.logger {}

abstract class TerminalBasePane(
    var config: TerminalConfig = TerminalConfig().apply {
        copyOnSelect = true
        enableClipboardNotice = false
    }
): TerminalIf, Pane() {

    private val outputProperty = SimpleObjectProperty<BufferedReader>()
    private val errorProperty  = SimpleObjectProperty<BufferedReader>()

    val webView = WebView()
    var interrupted = false
    var columns: Int = 2000
    var rows: Int = 1000

    var taskOutputReader: Task<*>? = null
    var taskErrorReader: Task<*>? = null

    var outputReader: BufferedReader
        get() = outputProperty.get()
        set(reader) {
            outputProperty.set(reader)
        }

    var errorReader: BufferedReader
        get() = errorProperty.get()
        set(reader) {
            errorProperty.set(reader)
        }

    private val terminal: JSObject
        get() = webView.engine.executeScript("t") as JSObject
    private val terminalIO: JSObject
        get() = webView.engine.executeScript("t.io") as JSObject
    private val window: JSObject
        get() = webView.engine.executeScript("window") as JSObject

    init {
        children.add(webView)
        outputProperty.addListener { _, _, reader -> taskOutputReader = runAsync { print(reader) } }
        errorProperty.addListener { _, _, reader -> taskErrorReader = runAsync { print(reader) } }
        webView.engine.loadWorker.stateProperty().addListener { _, _, _ ->
            window.setMember( "app", this )
        }
        webView.prefHeightProperty().bind(heightProperty())
        webView.prefWidthProperty().bind(widthProperty())
        webView.engine.loadContent(getContents())
    }

    private fun getContents(): String {
        val script = Classes.getResourceStream("/view/hterm/hterm_all.js").bufferedReader().readText()
        return StringBuilder().apply {
            Classes.getResourceStream("/view/hterm/hterm.html").bufferedReader().readLines().forEach { line ->
                if (("<script src=\"hterm_all.js\"></script>" == line)) {
                    append("<script>")
                    append(script)
                    append("</script>")
                } else {
                    append(line)
                }
            }
        }.toString()
    }

    override fun getPrefs(): String = Reflector.toJson(config,pretty=true)

    override fun updatePrefs(config: TerminalConfig) {
        if(this.config == config) return
        this.config = config
        runLater {
            window.call("updatePrefs", getPrefs())
        }
    }

    override fun resizeTerminal(columns: Int, rows: Int) {
        this.columns = columns
        this.rows    = rows
    }

    override fun copy(text: String) = Desktop.clipboard.set(text)

    private fun print(reader: Reader) {
        var nRead: Int
        val data = CharArray(1 * 1024)
        while (reader.read(data, 0, data.size).also { nRead = it } != -1) {
            val sb = StringBuilder(nRead)
            sb.append(data, 0, nRead)
            print(sb.toString())
        }
    }

    private fun print(text: String) = runLater {
        System.out.print(text)
        terminalIO.call("print", text)
    }

    fun focusCursor() = runLater {
        webView.requestFocus()
        terminal.call("focus")
    }

    fun closeReader() {
        taskOutputReader?.cancel()
        taskErrorReader?.cancel()
        outputReader.close()
        errorReader.close()
    }

    override fun onTerminalInit() {}
    override fun sendCommand(command: String) {}

}

