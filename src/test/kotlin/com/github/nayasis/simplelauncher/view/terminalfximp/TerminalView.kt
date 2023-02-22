package com.github.nayasis.simplelauncher.view.terminalfximp

import com.fasterxml.jackson.databind.ObjectMapper
import com.github.nayasis.kotlin.basica.core.io.delete
import com.github.nayasis.simplelauncher.view.terminalfximp.helper.ThreadHelper
import javafx.beans.property.ObjectProperty
import javafx.beans.property.ReadOnlyIntegerProperty
import javafx.beans.property.ReadOnlyIntegerWrapper
import javafx.beans.property.SimpleObjectProperty
import javafx.beans.value.ObservableValue
import javafx.concurrent.Worker
import javafx.scene.input.Clipboard
import javafx.scene.input.ClipboardContent
import javafx.scene.layout.Pane
import javafx.scene.web.WebEngine
import javafx.scene.web.WebView
import netscape.javascript.JSObject
import java.io.IOException
import java.io.Reader
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.util.*
import java.util.concurrent.CountDownLatch

open class TerminalView: Pane() {

    private val webView: WebView
    private val columnsProperty: ReadOnlyIntegerWrapper
    private val rowsProperty: ReadOnlyIntegerWrapper
    private val inputReaderProperty: ObjectProperty<Reader>
    private val errorReaderProperty: ObjectProperty<Reader>
    var terminalConfig = TerminalConfig()
        get() {
            if (Objects.isNull(field)) {
                field = TerminalConfig()
            }
            return field
        }
    protected val countDownLatch = CountDownLatch(1)

    init {
        initializeResources()
        webView = WebView()
        columnsProperty = ReadOnlyIntegerWrapper(150)
        rowsProperty = ReadOnlyIntegerWrapper(10)
        inputReaderProperty = SimpleObjectProperty()
        errorReaderProperty = SimpleObjectProperty()
        inputReaderProperty.addListener { observable: ObservableValue<out Reader>?, oldValue: Reader?, newValue: Reader ->
            ThreadHelper.start {
                printReader(
                    newValue
                )
            }
        }
        errorReaderProperty.addListener { observable: ObservableValue<out Reader>?, oldValue: Reader?, newValue: Reader ->
            ThreadHelper.start {
                printReader(
                    newValue
                )
            }
        }
        webView.engine.loadWorker.stateProperty()
            .addListener { observable: ObservableValue<out Worker.State?>?, oldValue: Worker.State?, newValue: Worker.State? ->
                window.setMember( "app", this )
            }
        webView.prefHeightProperty().bind(heightProperty())
        webView.prefWidthProperty().bind(widthProperty())
        val htmlPath = tempDirectory!!.resolve("hterm.html")
        webEngine().load(htmlPath.toUri().toString())
    }

    private fun initializeResources() {
        try {
            if (Objects.isNull(tempDirectory) || Files.notExists(tempDirectory)) {
                tempDirectory = Files.createTempDirectory("TerminalFX_Temp")
            }
        } catch (e: IOException) {
            throw RuntimeException(e)
        }
        val htmlPath = tempDirectory!!.resolve("hterm.html")
        if (Files.notExists(htmlPath)) {
            try {
                TerminalView::class.java.getResourceAsStream("/view/hterm/hterm.html").use { html ->
                    Files.copy(
                        html,
                        htmlPath,
                        StandardCopyOption.REPLACE_EXISTING
                    )
                }
            } catch (e: IOException) {
                throw RuntimeException(e)
            }
        }
        val htermJsPath = tempDirectory!!.resolve("hterm_all.js")
        if (Files.notExists(htermJsPath)) {
            try {
                TerminalView::class.java.getResourceAsStream("/view/hterm/hterm_all.js").use { html ->
                    Files.copy(
                        html,
                        htermJsPath,
                        StandardCopyOption.REPLACE_EXISTING
                    )
                }
            } catch (e: IOException) {
                throw RuntimeException(e)
            }
        }
    }

    @get:WebkitCall(from = "hterm")
    val prefs: String
        get() = try {
            ObjectMapper().writeValueAsString(terminalConfig)
        } catch (e: Exception) {
            throw RuntimeException(e)
        }

    fun updatePrefs(terminalConfig: TerminalConfig) {
        if (this.terminalConfig.equals(terminalConfig)) {
            return
        }
        this.terminalConfig = terminalConfig
        val prefs = prefs
        ThreadHelper.runActionLater({
            try {
                window.call("updatePrefs", prefs)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }, true)
    }

    @WebkitCall(from = "hterm")
    fun resizeTerminal(columns: Int, rows: Int) {
        columnsProperty.set(columns)
        rowsProperty.set(rows)
    }

    @WebkitCall
    fun onTerminalInit() {
        ThreadHelper.runActionLater({
            children.add(webView)
                                    }, true)
    }

    @WebkitCall
    open
        /**
         * Internal use only
         */
    fun onTerminalReady() {
        ThreadHelper.start {
            try {
                focusCursor()
                countDownLatch.countDown()
            } catch (e: Exception) {
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

    @WebkitCall(from = "hterm")
    fun copy(text: String?) {
        val clipboard = Clipboard.getSystemClipboard()
        val clipboardContent = ClipboardContent()
        clipboardContent.putString(text)
        clipboard.setContent(clipboardContent)
    }

    fun onTerminalFxReady(onReadyAction: Runnable?) {
        ThreadHelper.start {
            ThreadHelper.awaitLatch(countDownLatch)
            if (Objects.nonNull(onReadyAction)) {
                ThreadHelper.start(onReadyAction)
            }
        }
    }

    protected fun print(text: String?) {
        ThreadHelper.awaitLatch(countDownLatch)
        ThreadHelper.runActionLater { terminalIO.call("print", text) }
    }

    fun focusCursor() {
        ThreadHelper.runActionLater({
            webView.requestFocus()
            terminal.call("focus")
        }, true)
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

    fun columnsProperty(): ReadOnlyIntegerProperty {
        return columnsProperty.readOnlyProperty
    }

    val columns: Int
        get() = columnsProperty.get()

    fun rowsProperty(): ReadOnlyIntegerProperty {
        return rowsProperty.readOnlyProperty
    }

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

    companion object {
        private var tempDirectory: Path? = null

        init {
            Runtime.getRuntime().addShutdownHook(object: Thread() {
                override fun run() {
                    try {
                        if (Objects.nonNull(tempDirectory) && Files.exists(tempDirectory)) {
                            tempDirectory?.delete()
                        }
                    } catch (ex: IOException) {
                        ex.printStackTrace()
                    }
                }
            })
        }
    }
}