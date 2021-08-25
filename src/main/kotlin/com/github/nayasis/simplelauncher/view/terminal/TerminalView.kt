package com.github.nayasis.simplelauncher.view.terminal

import com.github.nayasis.kotlin.basica.core.klass.Classes
import com.github.nayasis.kotlin.basica.etc.error
import com.github.nayasis.kotlin.basica.reflection.Reflector
import javafx.application.Platform
import javafx.beans.property.ReadOnlyIntegerWrapper
import javafx.beans.property.SimpleObjectProperty
import javafx.scene.input.Clipboard
import javafx.scene.input.ClipboardContent
import javafx.scene.layout.Pane
import javafx.scene.web.WebView
import mu.KotlinLogging
import netscape.javascript.JSObject
import java.io.Reader

private val logger = KotlinLogging.logger {}

open class TerminalView(
    var config: TerminalConfig
): Pane() {

    protected val webView = WebView()

    private val columnsProperty = ReadOnlyIntegerWrapper(2000)
    private val rowsProperty = ReadOnlyIntegerWrapper(1000)

    private val inputReaderProperty = SimpleObjectProperty<Reader>()
    private val errorReaderProperty = SimpleObjectProperty<Reader>()

//    protected val countDownLatch = CountDownLatch(1)

    var columns: Int
        get() = columnsProperty.get()
        set(value) = columnsProperty.set(value)

    var rows: Int
        get() = rowsProperty.get()
        set(value) = rowsProperty.set(value)

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

    @get:WebkitCall
    val prefs: String
        get() = try {
            Reflector.toJson(config,pretty=true)
        } catch (e: Exception) {
            throw RuntimeException(e)
        }

    @WebkitCall
    fun updatePrefs(config: TerminalConfig) {
        if(this.config == config) return
        this.config = config
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
        this.columns = columns
        this.rows    = rows
    }

    @WebkitCall
    fun onTerminalInit() {
        Platform.runLater { children.add(webView) }
    }

    @WebkitCall
    open fun onTerminalReady() {
        focusCursor()
    }

    private fun print(reader: Reader) {
        try {
            var size: Int
            val buffer = CharArray(1 * 1024)
            while (reader.read(buffer, 0, buffer.size).also { size = it } != -1) {
                StringBuilder(size).append(buffer, 0, size).run { print(toString()) }
            }
        } catch (e: Exception) {
            logger.error(e)
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
//        runLater {
//            Threads.await(countDownLatch)
//            if (Objects.nonNull(onReadyAction)) {
//                Threads.run(onReadyAction)
//            }
//        }
    }

    protected fun print(text: String) {
        Platform.runLater { terminalIO.call("print", text) }
    }

    fun focusCursor() {
        Platform.runLater {
            webView.requestFocus()
            terminal.call("focus")
        }
//        runAsync {
//            webView.requestFocus()
//            terminal.call("focus")
//        }
//        Platform.runLater {
//            GlobalScope.launch {
//                webView.requestFocus()
//                terminal.call("focus")
//            }
//        }
    }

    private val terminal: JSObject
        private get() = webView.engine.executeScript("t") as JSObject
    private val terminalIO: JSObject
        private get() = webView.engine.executeScript("t.io") as JSObject
    val window: JSObject
        get() = webView.engine.executeScript("window") as JSObject

    init {
        inputReaderProperty.addListener { _, _, reader -> print(reader) }
        errorReaderProperty.addListener { _, _, reader -> print(reader) }
        webView.engine.loadWorker.stateProperty().addListener { _, _, _ ->
                window.setMember( "app", this )
            }
        webView.prefHeightProperty().bind(heightProperty())
        webView.prefWidthProperty().bind(widthProperty())
        webView.engine.loadContent(getContents())
    }

//    open fun runLater(runnable: Runnable) {
//        Platform.runLater{ Threads.run(runnable) }
//    }

}

annotation class WebkitCall(val from: String = "")

//object Threads {
//
//    private val executor = Executors.newCachedThreadPool()
//
//    fun await(latch: CountDownLatch) {
//        try {
//            latch.await()
//        } catch (e: InterruptedException) {
//            logger.error(e)
//        }
//    }
//
//    fun run(runnable: Runnable?) {
//        executor.submit(runnable)
//    }
//}
