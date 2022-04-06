package com.github.nayasis.simplelauncher.view.terminal

import com.github.nayasis.kotlin.basica.core.string.toResource
import com.github.nayasis.kotlin.basica.reflection.Reflector
import com.github.nayasis.kotlin.javafx.misc.Desktop
import com.github.nayasis.kotlin.javafx.misc.set
import javafx.beans.property.ReadOnlyIntegerWrapper
import javafx.beans.property.SimpleObjectProperty
import javafx.scene.layout.Pane
import javafx.scene.paint.Color
import javafx.scene.web.WebView
import mu.KotlinLogging
import netscape.javascript.JSObject
import tornadofx.runLater
import java.io.Reader
import java.util.concurrent.CountDownLatch
import kotlin.concurrent.thread

private val logger = KotlinLogging.logger {}

open class NTerminalView(
    var config: TerminalConfig = TerminalConfig().apply {
        backgroundColor = Color.rgb(16, 16, 16).toHex()
        foregroundColor = Color.rgb(240, 240, 240).toHex()
        cursorColor = Color.rgb(255, 0, 0, 0.5).toHex()
        scrollbarVisible = false
        scrollWhellMoveMultiplier = 0.5
        fontSize = 12
        cursorBlink = true
        copyOnSelect = true
        enableClipboardNotice = false
    }
): Pane() {

    private var inputReaderProperty = SimpleObjectProperty<Reader>()
    private var errorReaderProperty = SimpleObjectProperty<Reader>()

    private val webView = WebView()
    protected val countDownLatch = CountDownLatch(1)

    val columnsProperty = ReadOnlyIntegerWrapper(150)
    val rowsProperty    = ReadOnlyIntegerWrapper(10)

    var inputReader: Reader
        get() = inputReaderProperty.get()
        set(value) {
            inputReaderProperty.set(value)
        }

    var errorReader: Reader
        get() = errorReaderProperty.get()
        set(value) {
            errorReaderProperty.set(value)
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
        inputReaderProperty.addListener { _, _, reader -> thread(){ printReader(reader) } }
        errorReaderProperty.addListener { _, _, reader -> thread(){ printReader(reader) } }
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
            logger.debug { ">> is it called ??" }
            children.add(webView)
        }
    }


    @WebkitCall
    open fun onTerminalReady() {
        thread() {
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
        runLater {
            terminalIO.call("print", text)
        }
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

annotation class WebkitCall(val from: String = "")