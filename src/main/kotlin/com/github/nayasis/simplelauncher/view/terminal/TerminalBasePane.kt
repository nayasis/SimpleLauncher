package com.github.nayasis.simplelauncher.view.terminal

import com.github.nayasis.kotlin.basica.core.string.toResource
import com.github.nayasis.kotlin.basica.reflection.Reflector
import com.github.nayasis.kotlin.javafx.misc.Desktop
import com.github.nayasis.kotlin.javafx.misc.set
import javafx.beans.property.SimpleObjectProperty
import javafx.concurrent.Task
import javafx.scene.layout.Pane
import javafx.scene.web.WebView
import netscape.javascript.JSObject
import tornadofx.runAsync
import tornadofx.runLater
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.Reader

abstract class TerminalBasePane(
    var config: TerminalConfig = TerminalConfig().apply {
        cursorBlink = true
        copyOnSelect = true
        enableClipboardNotice = false
    }
): TerminalIf, Pane() {

    private val outputProperty = SimpleObjectProperty<BufferedReader?>()
    private val errorProperty  = SimpleObjectProperty<BufferedReader?>()
    private val inputProperty  = SimpleObjectProperty<BufferedWriter?>()

    val webView = WebView()
    var interrupted = false
    var columns: Int = 2000
    var rows: Int = 1000

    var taskOutputReader: Task<*>? = null
    var taskOutputError: Task<*>? = null

    var outputReader: BufferedReader?
        get() = outputProperty.get()
        set(reader) {
            outputProperty.set(reader)
        }

    var errorReader: BufferedReader?
        get() = errorProperty.get()
        set(reader) {
            errorProperty.set(reader)
        }

    var inputWriter: BufferedWriter?
        get() = inputProperty.get()
        set(writer) {
            inputProperty.set(writer)
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
        errorProperty.addListener { _, _, reader -> taskOutputError = runAsync { print(reader) } }
        webView.engine.loadWorker.stateProperty().addListener { _, _, _ ->
            window.setMember( "app", this )
        }
        webView.prefHeightProperty().bind(heightProperty())
        webView.prefWidthProperty().bind(widthProperty())
        webView.engine.load("/view/hterm/hterm.html".toResource()!!.toExternalForm())
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
        runCatching {
            while (reader.read(data, 0, data.size).also { nRead = it } != -1) {
                val sb = StringBuilder(nRead)
                sb.append(data, 0, nRead)
                print(sb.toString())
            }
        }
    }

    private fun print(text: String) = runLater {
        terminalIO.call("print", text)
    }

    fun focusCursor() = runLater {
        webView.requestFocus()
        terminal.call("focus")
    }

    protected fun closeReader() {
        taskOutputReader?.cancel()
        taskOutputError?.cancel()
        outputReader?.close()
        errorReader?.close()
        inputWriter?.close()
        outputProperty.set(null)
        errorProperty.set(null)
        inputProperty.set(null)
        webView.engine.load(null)
    }

    override fun onTerminalInit() {}

    override fun command(command: String) {
        print(command)
        inputWriter?.run {
            write(command)
            flush()
        }
    }

}

