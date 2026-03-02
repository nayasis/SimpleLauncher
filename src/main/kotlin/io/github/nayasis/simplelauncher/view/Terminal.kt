package io.github.nayasis.simplelauncher.view

import com.pty4j.PtyProcess
import com.pty4j.PtyProcessBuilder
import com.techsenger.jeditermfx.app.pty.PtyProcessTtyConnector
import com.techsenger.jeditermfx.core.TtyConnector
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
import java.util.concurrent.CountDownLatch
import kotlin.io.path.exists

private val logger = KotlinLogging.logger {}

@Suppress("unused")
class Terminal(
    command: Command,
    private val onAlways : ((terminal: Terminal) -> Unit)? = null,
    private val onFail   : ((error: Throwable)   -> Unit)? = null,
    private val onSuccess: ((terminal: Terminal) -> Unit)? = null,
): Stage() {

    private val processLatch = CountDownLatch(1)
    private val terminal     = createTerminalWidget(command, processLatch)

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

    fun kill() {
        runCatching {
            terminal.ttyConnector.close()
        }.onFailure { e ->
            logger.error(e) { "Failed to kill process" }
        }
    }

    fun isProcessRunning(): Boolean {
        return runCatching {
            terminal.ttyConnector.isConnected
        }.getOrDefault(false)
    }

    private fun createTerminalWidget(cmd: Command, processLatch: CountDownLatch): JediTermFxWidget {
        return JediTermFxWidget(80, 200, BlackTerminalTheme()).apply {
            ttyConnector = DelayedTtyConnector(cmd, processLatch)
            addHyperlinkFilter(DefaultHyperlinkFilter())
            start()
            addListener {
                config.stageTerminal = StageProperty(this@Terminal)
                close()
            }
        }
    }

    private fun waitFor() {
        try {
            processLatch.countDown()
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

    private fun Pane.bindSizeProperties(other: Pane) {
        prefWidthProperty().bind(other.widthProperty())
        prefHeightProperty().bind(other.heightProperty())
        minWidthProperty().bind(other.minWidthProperty())
        minHeightProperty().bind(other.minHeightProperty())
        maxWidthProperty().bind(other.maxWidthProperty())
        maxHeightProperty().bind(other.maxHeightProperty())
    }

}

private class DelayedTtyConnector(
    private val cmd: Command,
    private val latch: CountDownLatch
): TtyConnector {

    private lateinit var ttyConnector: PtyProcessTtyConnector

    private fun ensureInitialized() {
        if (!::ttyConnector.isInitialized) {
            latch.await()
            ttyConnector = PtyProcessTtyConnector(toPtyProcess(cmd), StandardCharsets.UTF_8)
        }
    }

    override fun read(buf: CharArray, offset: Int, length: Int): Int {
        ensureInitialized()
        return ttyConnector.read(buf, offset, length)
    }

    override fun write(bytes: ByteArray) {
        if (::ttyConnector.isInitialized) ttyConnector.write(bytes)
    }

    override fun write(string: String) {
        if (::ttyConnector.isInitialized) ttyConnector.write(string)
    }

    override fun isConnected(): Boolean =
        if (::ttyConnector.isInitialized) ttyConnector.isConnected else true

    override fun waitFor(): Int {
        ensureInitialized()
        return ttyConnector.waitFor()
    }

    override fun ready(): Boolean =
        if (::ttyConnector.isInitialized) ttyConnector.ready() else false

    override fun getName(): String = "Delayed"

    override fun close() {
        if (::ttyConnector.isInitialized) ttyConnector.close()
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

}