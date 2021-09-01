package com.github.nayasis.simplelauncher.view.terminal

import com.github.nayasis.kotlin.basica.core.klass.Classes
import com.github.nayasis.kotlin.basica.reflection.Reflector
import com.github.nayasis.kotlin.javafx.misc.Desktop
import com.github.nayasis.kotlin.javafx.misc.set
import javafx.application.Platform
import javafx.beans.property.SimpleObjectProperty
import javafx.scene.layout.Pane
import javafx.scene.web.WebView
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import mu.KotlinLogging
import netscape.javascript.JSObject
import java.io.BufferedReader
import java.io.Reader

private val logger = KotlinLogging.logger {}

abstract class TerminalView(
    var config: TerminalConfig
): TerminalIf, Pane() {

    protected val webView = WebView()

//    private val outputProperty = SimpleObjectProperty<BufferedReader>()
//    private val errorProperty = SimpleObjectProperty<BufferedReader>()

    var columns: Int = 2000
    var rows: Int = 1000

    var outputReader: BufferedReader? = null
//        get() = outputProperty.get()
//        set(reader) {
//            outputProperty.set(reader)
//        }

    private val terminal: JSObject
        get() = webView.engine.executeScript("t") as JSObject
    private val terminalIO: JSObject
        get() = webView.engine.executeScript("t.io") as JSObject
    val window: JSObject
        get() = webView.engine.executeScript("window") as JSObject

    init {
        children.add(webView)
//        outputProperty.addListener { _, _, reader -> print(reader.readText()) }
        webView.engine.loadWorker.stateProperty().addListener { _, _, _ ->
            window.setMember( "app", this )
        }
        webView.prefHeightProperty().bind(heightProperty())
        webView.prefWidthProperty().bind(widthProperty())
        webView.engine.loadContent(getContents())

    }

    fun readOutput() {

        GlobalScope.launch {
            outputReader?.let{ reader ->
                print(reader.readText())
            }
        }

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

    override fun getPrefs(): String {
        return Reflector.toJson(config,pretty=true)
    }

    @WebkitCall
    override fun updatePrefs(config: TerminalConfig) {
        if(this.config == config) return
        this.config = config
        Platform.runLater {
            window.call("updatePrefs", getPrefs())
        }
    }

    @WebkitCall
    override fun resizeTerminal(columns: Int, rows: Int) {
        this.columns = columns
        this.rows    = rows
    }

    override fun copy(text: String) = Desktop.clipboard.set(text)

    protected fun print(text: String) {
        Platform.runLater { terminalIO.call("print", text) }
    }

    fun focusCursor() {
        Platform.runLater {
            webView.requestFocus()
            terminal.call("focus")
        }
    }

    override fun onTerminalInit() {}

}

