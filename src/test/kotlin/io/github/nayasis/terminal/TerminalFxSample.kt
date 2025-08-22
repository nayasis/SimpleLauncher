package io.github.nayasis.terminal

import com.pty4j.PtyProcessBuilder
import com.techsenger.jeditermfx.app.pty.PtyProcessTtyConnector
import com.techsenger.jeditermfx.core.TerminalColor
import com.techsenger.jeditermfx.ui.DefaultHyperlinkFilter
import com.techsenger.jeditermfx.ui.JediTermFxWidget
import com.techsenger.jeditermfx.ui.settings.DefaultSettingsProvider
import javafx.stage.Stage
import mu.KotlinLogging
import tornadofx.*
import java.nio.charset.StandardCharsets

private val logger = KotlinLogging.logger {}

fun main(vararg args: String) {
    launch<TerminalFxSample>(*args)
}

class TerminalFxSample: App(TerminalFxSampleView::class) {
    override fun start(stage: Stage) {
        super.start(stage)
        // set window size to 300 x 200
        stage.apply {
            width     = 500.0
            height    = 400.0
            minWidth  = width
            minHeight = height
        }
    }
}

class TerminalFxSampleView : View("TerminalFx Sample") {

    override val root = vbox(spacing = 0) {

        val widget = createTerminal()
        widget.pane.also {
            it.prefWidthProperty().bind(widthProperty())
            it.prefHeightProperty().bind(heightProperty())
            it.minWidthProperty().bind(minWidthProperty())
            it.minHeightProperty().bind(minHeightProperty())
            it.maxWidthProperty().bind(maxWidthProperty())
            it.maxHeightProperty().bind(maxHeightProperty())
        }
        widget.pane.attachTo(this)

        runLater {
            runAsync {
                widget.ttyConnector.waitFor()
                runLater {
                    titleProperty.set("Done ${titleProperty.get()}")
                }
            }
        }
    }

}

private fun createTerminal(): JediTermFxWidget {
    return JediTermFxWidget(80, 200, DefaultSettingsProvider()).apply {
        ttyConnector = createTtyConnector()
        addHyperlinkFilter(DefaultHyperlinkFilter())
        start()
    }
}

private class DarkThemeSettingsProvider : DefaultSettingsProvider() {
    override fun getDefaultBackground(): TerminalColor {
        return TerminalColor(0, 0, 0)
    }

    override fun getDefaultForeground(): TerminalColor {
        return TerminalColor(255, 255, 255)
    }
}

private fun createTtyConnector(): PtyProcessTtyConnector {
    try {
//        val command = listOf("ls", "-al")
//        val command = listOf("cmd.exe", "/c", "echo", "Hello", "&&", "timeout", "/t", "30")
//        val command = listOf("cmd.exe")
        val command = listOf("c:/project_ref/test/test.exe", "10")
//        val envs = System.getenv().toMutableMap().apply {
//            put("TERM", "xterm-256color")
//        }
        val process = PtyProcessBuilder().setCommand(command.toTypedArray())
//            .setEnvironment(envs)
            .start()

        return PtyProcessTtyConnector(process, StandardCharsets.UTF_8)
    } catch (e: Exception) {
        throw kotlin.IllegalStateException(e)
    }
}