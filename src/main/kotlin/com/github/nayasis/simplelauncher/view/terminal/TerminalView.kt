package com.github.nayasis.simplelauncher.view.terminal

import com.fasterxml.jackson.databind.ObjectMapper
import com.github.nayasis.kotlin.basica.core.klass.Classes
import com.github.nayasis.kotlin.basica.etc.error
import javafx.application.Platform
import javafx.beans.property.ObjectProperty
import javafx.beans.property.ReadOnlyIntegerWrapper
import javafx.beans.property.SimpleObjectProperty
import javafx.beans.value.ObservableValue
import javafx.concurrent.Worker
import javafx.scene.input.Clipboard
import javafx.scene.input.ClipboardContent
import javafx.scene.layout.Pane
import javafx.scene.web.WebEngine
import javafx.scene.web.WebView
import mu.KotlinLogging
import netscape.javascript.JSObject
import java.io.Reader
import java.util.*
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors

private val logger = KotlinLogging.logger {}

open class TerminalView: Pane() {

    protected val webView = WebView()

    val columnsProperty = ReadOnlyIntegerWrapper(2000)
    val rowsProperty = ReadOnlyIntegerWrapper(1000)

    private val inputReaderProperty: ObjectProperty<Reader> = SimpleObjectProperty()
    private val errorReaderProperty: ObjectProperty<Reader> = SimpleObjectProperty()
    protected val countDownLatch = CountDownLatch(1)

    var terminalConfig = TerminalConfig()

    private val contents: String
        private get() {

            val script =  Classes.getResourceStream("/view/hterm/hterm_all.js").bufferedReader().readText()
            val sb = StringBuilder()

            Classes.getResourceStream("/view/hterm/hterm.html").bufferedReader().readLines().forEach { readline ->
                if (("<script src=\"hterm_all.js\"></script>" == readline)) {
                    sb.append("<script>")
                    sb.append(script)
                    sb.append("</script>")
                } else {
                    sb.append(readline)
                }
            }
            return sb.toString()
        }

    @get:WebkitCall
    val prefs: String
        get() = try {
            ObjectMapper().writeValueAsString(terminalConfig)
        } catch (e: Exception) {
            throw RuntimeException(e)
        }

    @WebkitCall
    fun updatePrefs(terminalConfig: TerminalConfig) {
        if (this.terminalConfig == terminalConfig) {
            return
        }
        this.terminalConfig = terminalConfig
        val prefs = prefs
        Platform.runLater {
            try {
                window.call("updatePrefs", prefs)
            } catch (e: Exception) {
                logger.error(e)
            }
        }
    }

    @WebkitCall
    fun resizeTerminal(columns: Int, rows: Int) {
        columnsProperty.set(columns)
        rowsProperty.set(rows)
    }

    @WebkitCall
    fun onTerminalInit() {
        Platform.runLater { getChildren().add(webView) }
    }

    @WebkitCall
    open fun onTerminalReady() {
        run {
            try {
                focusCursor()
                countDownLatch.countDown()
            } catch (e: Exception) {
                logger.error(e.message, e)
            }
        }
    }

    private fun printReader(bufferedReader: Reader) {
        try {
            var nRead: Int
            val data = CharArray(1 * 1024)
            while (bufferedReader.read(data, 0, data.size).also { nRead = it } != -1) {
                val builder = StringBuilder(nRead)
                builder.append(data, 0, nRead)
                print(builder.toString())
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    @WebkitCall
    fun copy(text: String?) {
        val clipboard = Clipboard.getSystemClipboard()
        val clipboardContent = ClipboardContent()
        clipboardContent.putString(text)
        clipboard.setContent(clipboardContent)
    }

    fun onTerminalFxReady(onReadyAction: Runnable) {
        run {
            Threads.await(countDownLatch)
            if (Objects.nonNull(onReadyAction)) {
                run(onReadyAction)
            }
        }
    }

    protected fun print(text: String?) {
        Platform.runLater { terminalIO.call("print", text) }
    }

    fun focusCursor() {
        Platform.runLater {
            webView.requestFocus()
            terminal.call("focus")
        }
    }

    private val terminal: JSObject
        private get() = webEngine().executeScript("t") as JSObject
    private val terminalIO: JSObject
        private get() = webEngine().executeScript("t.io") as JSObject
    val window: JSObject
        get() = webEngine().executeScript("window") as JSObject

    private fun webEngine(): WebEngine {
        return webView.engine
    }

    val columns: Int
        get() = columnsProperty.get()

    val rows: Int
        get() = rowsProperty.get()

    fun inputReaderProperty(): ObjectProperty<Reader> {
        return inputReaderProperty
    }

    var inputReader: Reader
        get() = inputReaderProperty.get()
        set(reader) {
            inputReaderProperty.set(reader)
        }

    fun errorReaderProperty(): ObjectProperty<Reader> {
        return errorReaderProperty
    }

    var errorReader: Reader
        get() = errorReaderProperty.get()
        set(reader) {
            errorReaderProperty.set(reader)
        }

    init {
        inputReaderProperty.addListener { _, _, newValue: Reader ->
            run {
                printReader(
                    newValue
                )
            }
        }
        errorReaderProperty.addListener { _, _, newValue: Reader ->
            run {
                printReader(
                    newValue
                )
            }
        }
        webView.engine.loadWorker.stateProperty()
            .addListener { _, _, _ ->
                window.setMember(
                    "app",
                    this
                )
            }
        webView.prefHeightProperty().bind(heightProperty())
        webView.prefWidthProperty().bind(widthProperty())
        val webEngine = webEngine()
        webEngine.loadContent(contents)
    }

    protected fun run(runnable: Runnable) {
        Platform.runLater {
            Threads.run(runnable)
        }
    }

}

annotation class WebkitCall(val from: String = "")

object Threads {

    internal val executor = Executors.newCachedThreadPool()

    fun await(latch: CountDownLatch) {
        try {
            latch.await()
        } catch (e: InterruptedException) {
            logger.error(e)
        }
    }

    fun run(runnable: Runnable) {
        executor.submit(runnable)
    }
}
