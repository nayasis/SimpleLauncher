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
import java.util.concurrent.CountDownLatch

open class TerminalView(
    var config: TerminalConfig = TerminalConfig().apply {
        cursorBlink = true
        copyOnSelect = true
        enableClipboardNotice = false
    }
): Pane() {

    private var inputReaderProperty = SimpleObjectProperty<Reader>()
    private var errorReaderProperty = SimpleObjectProperty<Reader>()

    private val webView = WebView()

    private val columnsProperty = ReadOnlyIntegerWrapper(150)
    private val rowsProperty    = ReadOnlyIntegerWrapper(10)

    private val countDownLatch = CountDownLatch(1)

    var inputReader: Reader
        get() = inputReaderProperty.get()
        set(reader) {
            inputReaderProperty.set(reader)
        }

    var errorReader: Reader
        get() = errorReaderProperty.get()
        set(reader) {
            errorReaderProperty.set(reader)
        }

    var row: Int
        get() = rowsProperty.get()
        set(value) = rowsProperty.set(value)

    var column: Int
        get() = columnsProperty.get()
        set(value) = columnsProperty.set(value)

    private val terminal: JSObject
        get() = webView.engine.executeScript("t") as JSObject
    private val terminalIO: JSObject
        get() = webView.engine.executeScript("t.io") as JSObject
    private val window: JSObject
        get() = webView.engine.executeScript("window") as JSObject

    init {
        inputReaderProperty.addListener { _, _, reader -> runLater{ print(reader) } }
        errorReaderProperty.addListener { _, _, reader -> runLater{ print(reader) } }
        webView.engine.loadWorker.stateProperty().addListener { _, _, _ -> window.setMember("app", this) }
        webView.prefHeightProperty().bind(heightProperty())
        webView.prefWidthProperty().bind(widthProperty())
        webView.engine.load("/view/hterm/hterm.html".toResource()!!.toExternalForm())
    }

    @WebkitCall(from = "hterm")
    fun getPrefs(): String = Reflector.toJson(config,pretty=true)

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

    @WebkitCall(from = "hterm")
    fun resizeTerminal(columns: Int, rows: Int) {
        columnsProperty.set(columns)
        rowsProperty.set(rows)
    }

    @WebkitCall
    fun onTerminalInit() {
        runLater {
            children.add(webView)
        }
    }


    @WebkitCall
    fun onTerminalReady() {
        runLater {
            focusCursor()
            countDownLatch.countDown()
        }
    }

    private fun printReader(reader: Reader) {
        try {
            var nRead: Int
            val data = CharArray(1 * 1024)
            while (reader.read(data, 0, data.size).also { nRead = it } != -1) {
                val builder = StringBuilder(nRead)
                builder.append(data, 0, nRead)
                print(builder.toString())
            }
        } catch (e: Throwable) {
            e.printStackTrace()
        }
    }

    private fun print(text: String) {
        countDownLatch.await()
        terminalIO.call("print", text)
    }

    @WebkitCall(from = "hterm")
    fun copy(text: String) = Desktop.clipboard.set(text)

    fun onTerminalFxReady(fn: (()->Unit)?) {
        runLater {
            countDownLatch.await()
            fn?.invoke()
        }
    }

}