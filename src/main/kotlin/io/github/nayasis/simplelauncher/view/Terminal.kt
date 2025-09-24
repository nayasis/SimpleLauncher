package io.github.nayasis.simplelauncher.view

import com.pty4j.PtyProcess
import com.pty4j.PtyProcessBuilder
import com.techsenger.jeditermfx.app.pty.PtyProcessTtyConnector
import com.techsenger.jeditermfx.ui.DefaultHyperlinkFilter
import com.techsenger.jeditermfx.ui.JediTermFxWidget
import io.github.nayasis.kotlin.basica.core.string.toPath
import io.github.nayasis.kotlin.basica.etc.error
import io.github.nayasis.kotlin.basica.exec.Command
import io.github.nayasis.kotlin.javafx.misc.runAwait
import io.github.nayasis.kotlin.javafx.property.StageProperty
import io.github.nayasis.simplelauncher.common.Context.Companion.config
import io.github.nayasis.simplelauncher.view.theme.BlackTerminalTheme
import io.github.oshai.kotlinlogging.KotlinLogging
import javafx.scene.Scene
import javafx.scene.layout.Pane
import javafx.stage.Stage
import tornadofx.attachTo
import tornadofx.runLater
import tornadofx.vbox
import java.nio.charset.StandardCharsets
import kotlin.io.path.exists

private val logger = KotlinLogging.logger {}

@Suppress("unused")
class Terminal(
    command: Command,
    private val onAlways : ((terminal: Terminal) -> Unit)? = null,
    private val onFail   : ((error: Throwable)   -> Unit)? = null,
    private val onSuccess: ((terminal: Terminal) -> Unit)? = null,
): Stage() {

    private val terminal = toTerminalWidget(command)

    init {

        scene = Scene(vbox(spacing = 0) {
            terminal.pane.also {
                it.bindSizeProperties(this@vbox)
            }
            terminal.pane.attachTo(this@vbox)
        })

        setOnShown {
            title     = "$command"
            width     = 700.0
            height    = 600.0
            minWidth  = 100.0
            minHeight = 100.0
            config.stageTerminal?.bind(this)
            runAwait {
                waitFor()
            }
        }

        setOnCloseRequest {
            runCatching { terminal.ttyConnector.close() }.onFailure { e -> logger.error(e) }
            runCatching { terminal.close() }.onFailure { e -> logger.error(e) }
        }

    }

    fun sendCommand(command: Command) {
        terminal.ttyConnector.write("$command\r\n")
    }

    fun sendCommand(command: String) {
        terminal.ttyConnector.write("$command\r\n")
    }

    private fun waitFor() {
        try {
            terminal.ttyConnector.waitFor()
            onSuccess?.also { f ->
                runCatching{ f.invoke(this) }.onFailure { e -> logger.error(e) }
            }
            runLater { title = "Done - $title" }
        } catch (e: Exception) {
            logger.error(e)
            onFail?.also { f ->
                runCatching{ f.invoke( e) }.onFailure { ex -> logger.error(ex) }
            }
        } finally {
            onAlways?.also { f ->
                runCatching{ f.invoke(this) }.onFailure { e -> logger.error(e) }
            }
        }
    }

    private fun toTerminalWidget(cmd: Command): JediTermFxWidget {
        return JediTermFxWidget(80, 200, BlackTerminalTheme()).apply {
            this.ttyConnector = PtyProcessTtyConnector(toPtyProcess(cmd), StandardCharsets.UTF_8)
            this.addHyperlinkFilter(DefaultHyperlinkFilter())
            this.start()
        }.also { widget ->
            // close event
            widget.addListener {
                config.stageTerminal = StageProperty(this)
                widget.close()
            }
        }
    }

    private fun toPtyProcess(cmd: Command): PtyProcess {
        val envs = System.getenv().toMutableMap().apply {
            put("TERM", "xterm-256color")
        }
        return PtyProcessBuilder()
            .setCommand(cmd.command.toTypedArray())
            .setEnvironment(envs)
            .also { builder ->
                if(cmd.workingDirectory?.toPath()?.exists() == true) {
                    builder.setDirectory(cmd.workingDirectory)
                }
            }
            .start()
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