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

    private val outputProperty = SimpleObjectProperty<Reader?>()
    private val errorProperty  = SimpleObjectProperty<Reader?>()
    private val inputProperty  = SimpleObjectProperty<Writer?>()

    val columnsProperty = ReadOnlyIntegerWrapper(150)
    val rowsProperty    = ReadOnlyIntegerWrapper(10)

    private val webView = WebView()
    protected val countDownLatch = CountDownLatch(1)

    var interrupted = false
    var columns: Int = 2000
    var rows: Int = 1000

    var outputReader: Reader?
        get() = outputProperty.get()
        set(value) = outputProperty.set(value)

    var errorReader: Reader?
        get() = errorProperty.get()
        set(value) = errorProperty.set(value)

    var inputWriter: Writer?
        get() = inputProperty.get()
        set(value) = inputProperty.set(value)

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
        outputProperty.addListener { _, _, reader -> thread() { print(reader) } }
        errorProperty.addListener  { _, _, reader -> thread() { print(reader) } }
        webView.engine.loadWorker.stateProperty().addListener { _,_,_ ->
            window.setMember( "app", this )
        }
        webView.prefHeightProperty().bind(heightProperty())
        webView.prefWidthProperty().bind(widthProperty())
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
        countDownLatch.await()
        terminalIO.call("print", text)
    }

    protected fun closeReader() {
        webView.engine.load(null)
    }

    override fun command(command: String?) {
        if(command.isNullOrEmpty()) return
        commandQueue.put(command)
        thread() {
            commandQueue.poll().let { cmd ->
                inputWriter?.run {
                    write(cmd)
                    flush()
                }
            }
        }
    }

}

