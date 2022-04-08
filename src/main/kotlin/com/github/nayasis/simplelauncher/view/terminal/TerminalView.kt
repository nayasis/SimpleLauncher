package com.github.nayasis.simplelauncher.view.terminal

import com.github.nayasis.kotlin.basica.core.string.toResource
import com.github.nayasis.kotlin.basica.reflection.Reflector
import com.github.nayasis.kotlin.javafx.misc.Desktop
import com.github.nayasis.kotlin.javafx.misc.set
import javafx.beans.property.ReadOnlyIntegerWrapper
import javafx.beans.property.SimpleObjectProperty
import javafx.scene.layout.Pane
import javafx.scene.web.WebView
import netscape.javascript.JSObject
import tornadofx.runLater
import java.io.Reader
import java.io.Writer
import java.util.concurrent.CountDownLatch
import java.util.concurrent.LinkedBlockingQueue
import kotlin.concurrent.thread

abstract class TerminalView(
    var config: TerminalConfig
): TerminalIf, Pane() {

    private val inputReaderProperty  = SimpleObjectProperty<Reader?>()
    private val errorReaderProperty  = SimpleObjectProperty<Reader?>()
    private val outputWriterProperty = SimpleObjectProperty<Writer?>()

    val columnsProperty = ReadOnlyIntegerWrapper(2000)
    val rowsProperty    = ReadOnlyIntegerWrapper(1000)

    private val webView = WebView()
    protected val countDownLatch = CountDownLatch(1)

//    var interrupted = false

    var inputReader: Reader?
        get() = inputReaderProperty.get()
        set(value) = inputReaderProperty.set(value)

    var errorReader: Reader?
        get() = errorReaderProperty.get()
        set(value) = errorReaderProperty.set(value)

    var outputWriter: Writer?
        get() = outputWriterProperty.get()
        set(value) = outputWriterProperty.set(value)

    var row: Int
        get() = rowsProperty.get()
        set(value) = rowsProperty.set(value)

    var column: Int
        get() = columnsProperty.get()
        set(value) = columnsProperty.set(value)

    private val commandQueue = LinkedBlockingQueue<String>()

    private val terminal: JSObject
        get() = webView.engine.executeScript("t") as JSObject
    private val terminalIO: JSObject
        get() = webView.engine.executeScript("t.io") as JSObject
    private val window: JSObject
        get() = webView.engine.executeScript("window") as JSObject

    init {
        inputReaderProperty.addListener  { _, _, reader -> reader?.let{ thread() { printReader(it) } } }
        errorReaderProperty.addListener  { _, _, reader -> reader?.let{ thread() { printReader(it) } } }
        webView.prefHeightProperty().bind(heightProperty())
        webView.prefWidthProperty().bind(widthProperty())
        webView.engine.loadWorker.stateProperty().addListener { _,_,_ ->
            window.setMember( "app", this )
        }
        webView.engine.load("/view/hterm/hterm.html".toResource()!!.toExternalForm())
    }

    override fun getPrefs(): String = Reflector.toJson(config,pretty=true)

    fun updatePrefs(config: TerminalConfig) {
        if(this.config == config) return
        this.config = config
        runLater {
            window.call("updatePrefs", getPrefs())
        }
    }

    fun focusCursor() {
        runLater {
            webView.requestFocus()
            terminal.call("focus")
        }
    }

    override fun resizeTerminal(columns: Int, rows: Int) {
        columnsProperty.set(columns)
        rowsProperty.set(rows)
    }

    override fun onTerminalInit() {
        runLater {
            children.add(webView)
        }
    }

    override fun copy(text: String) = Desktop.clipboard.set(text)

    private fun printReader(reader: Reader) {
        var n: Int
        val data = CharArray(1 * 1024)
        runCatching {
            while (reader.read(data, 0, data.size).also { n = it } != -1) {
                val sb = StringBuilder(n)
                sb.append(data, 0, n)
                print(sb.toString())
            }
        }
    }

    private fun print(text: String) = runLater {
        countDownLatch.await()
        terminalIO.call("print", text)
    }

    open fun close() {
        inputReaderProperty.set(null)
        errorReaderProperty.set(null)
        outputWriterProperty.set(null)
        webView.engine.load(null)
    }

    override fun command(command: String?) {
        if(command.isNullOrEmpty()) return
        commandQueue.put(command)
        thread() {
            commandQueue.poll().let { cmd ->
                outputWriter?.run {
                    write(cmd)
                    flush()
                }
            }
        }
    }

}

