package io.github.nayasis.terminal

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import com.pty4j.PtyProcessBuilder
import com.techsenger.jeditermfx.app.pty.PtyProcessTtyConnector
import com.techsenger.jeditermfx.ui.DefaultHyperlinkFilter
import com.techsenger.jeditermfx.ui.JediTermFxWidget
import io.github.nayasis.terminal.theme.BlackTerminalTheme
import io.github.oshai.kotlinlogging.KotlinLogging
import javafx.scene.layout.Pane
import javafx.stage.Stage
import org.slf4j.LoggerFactory
import tornadofx.*
import tornadofx.attachTo
import tornadofx.vbox
import java.nio.charset.StandardCharsets
import kotlin.also
import kotlin.apply
import kotlin.collections.forEach
import kotlin.collections.toMutableMap
import kotlin.collections.toTypedArray

private val logger = KotlinLogging.logger {}

fun main(vararg args: String) {
    configureLogging()
    launch<TerminalFxSample>(*args)
}

private fun configureLogging() {
    listOf(
        "com.techsenger.jeditermfx.core",
        "com.techsenger.jeditermfx.ui",
        "com.techsenger.jeditermfx",
        "com.pty4j",
    ).forEach { packageName ->
        (LoggerFactory.getLogger(packageName) as Logger).level = Level.WARN
    }
}

class TerminalFxSample: App(TerminalFxSampleView::class) {
    override fun start(stage: Stage) {
        super.start(stage)
        // set window size for better terminal display with Korean text
        stage.apply {
            width     = 450.0
            height    = 400.0
            minWidth  = 100.0
            minHeight =  40.0
        }

    }
}

class TerminalFxSampleView : View("JediTermFx Sample") {

    private var terminalWidget = createTerminal()

    override val root = vbox(spacing = 0) {
        terminalWidget.pane.also {
            it.bindSizeProperties(this@vbox)
        }
        terminalWidget.pane.attachTo(this)
        runLater {
            runAsync {
                terminalWidget.ttyConnector.write("dir\r")
            }
            runAsync {
                terminalWidget.ttyConnector.waitFor()
                runLater {
                    titleProperty.set("Done - ${titleProperty.get()}")
                }
            }
        }
    }

    override fun onUndock() {
        terminalWidget.close()
        super.onUndock()
    }

    private fun Pane.bindSizeProperties(other: Pane) {
        prefWidthProperty().bind(other.widthProperty())
        prefHeightProperty().bind(other.heightProperty())
        minWidthProperty().bind(other.minWidthProperty())
        minHeightProperty().bind(other.minHeightProperty())
        maxWidthProperty().bind(other.maxWidthProperty())
        maxHeightProperty().bind(other.maxHeightProperty())
    }

}

private fun createTerminal(): JediTermFxWidget {
    return JediTermFxWidget(80, 200, BlackTerminalTheme()).apply {  // 더 넓은 터미널
        ttyConnector = createTtyConnector()
        addHyperlinkFilter(DefaultHyperlinkFilter())
        start()
    }
}

private fun createTtyConnector(): PtyProcessTtyConnector {
    // Windows command example
    val command = listOf("cmd")
//    val command = listOf("src/test/resources/test-program/test.exe", "5")
    val envs = System.getenv().toMutableMap().apply {
        put("TERM", "xterm-256color")
    }

    val process = PtyProcessBuilder()
        .setCommand(command.toTypedArray())
        .setEnvironment(envs)
        .start()

    return PtyProcessTtyConnector(process, StandardCharsets.UTF_8)
}