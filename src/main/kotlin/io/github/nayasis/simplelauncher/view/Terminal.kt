package io.github.nayasis.simplelauncher.view

import com.pty4j.PtyProcess
import com.pty4j.PtyProcessBuilder
import com.techsenger.jeditermfx.app.pty.PtyProcessTtyConnector
import com.techsenger.jeditermfx.core.model.StyleState
import com.techsenger.jeditermfx.core.model.TerminalTextBuffer
import com.techsenger.jeditermfx.ui.DefaultHyperlinkFilter
import com.techsenger.jeditermfx.ui.JediTermFxWidget
import com.techsenger.jeditermfx.ui.TerminalPanel
import com.techsenger.jeditermfx.ui.settings.SettingsProvider
import io.github.nayasis.kotlin.basica.core.string.toPath
import io.github.nayasis.kotlin.basica.etc.error
import io.github.nayasis.kotlin.basica.exec.Command
import io.github.nayasis.kotlin.javafx.misc.runAwait
import io.github.nayasis.kotlin.javafx.property.InsetProperty
import io.github.nayasis.kotlin.javafx.property.StageProperty
import io.github.nayasis.kotlin.javafx.stage.watchMaximized
import io.github.nayasis.simplelauncher.common.Context.Companion.config
import io.github.nayasis.simplelauncher.view.theme.BlackTerminalTheme
import io.github.oshai.kotlinlogging.KotlinLogging
import javafx.animation.KeyFrame
import javafx.animation.Timeline
import javafx.event.EventHandler
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
    private val onAlways : ((terminal: Terminal) -> Unit)? = null,
    private val onFail   : ((error: Throwable)   -> Unit)? = null,
    private val onSuccess: ((terminal: Terminal) -> Unit)? = null,
): Stage() {

    private lateinit var terminal: JediTermFxWidget

    private val vbox = vbox(spacing = 0)

    init {

        scene = Scene(vbox)

        config.stageTerminal?.sanitizeTerminalStageProperty()?.bind(this) ?: run {
            width     = 700.0
            height    = 600.0
            minWidth  = 100.0
            minHeight = 100.0
        }
        watchMaximized()

        setOnCloseRequest {
            persistStageState()
            runCatching { terminal.ttyConnector.close() }.onFailure { e -> logger.error(e) }
            runCatching { terminal.close() }.onFailure { e -> logger.error(e) }
        }
        setOnHidden {
            persistStageState()
        }

    }

    fun run(command: Command) {
        title    = "$command"
        terminal = toTerminalWidget(command)
        vbox.apply {
            terminal.pane.also {
                it.bindSizeProperties(this)
            }
            terminal.pane.attachTo(this)
        }
        runAwait {
            waitFor()
        }
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
        return SafeJediTermFxWidget(80, 200, BlackTerminalTheme()).apply {
            this.ttyConnector = PtyProcessTtyConnector(toPtyProcess(cmd), StandardCharsets.UTF_8)
            this.addHyperlinkFilter(DefaultHyperlinkFilter())
            this.start()
        }.also { widget ->
            // close event
            widget.addListener {
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

    private fun persistStageState() {
        config.stageTerminal = StageProperty(this, includeChildren = false)
        config.save()
    }

}

private class SafeJediTermFxWidget(
    columns: Int,
    lines: Int,
    settingsProvider: SettingsProvider,
): JediTermFxWidget(columns, lines, settingsProvider) {

    override fun createTerminalPanel(
        settingsProvider: SettingsProvider,
        styleState: StyleState,
        terminalTextBuffer: TerminalTextBuffer,
    ): TerminalPanel {
        return SafeTerminalPanel(settingsProvider, terminalTextBuffer, styleState)
    }

}

private class SafeTerminalPanel(
    settingsProvider: SettingsProvider,
    terminalTextBuffer: TerminalTextBuffer,
    styleState: StyleState,
): TerminalPanel(settingsProvider, terminalTextBuffer, styleState) {

    override fun init() {
        super.init()
        patchWeakRedrawTimer()
    }

    private fun patchWeakRedrawTimer() {
        runCatching {
            val field = TerminalPanel::class.java.getDeclaredField("myRepaintTimeLine").apply {
                isAccessible = true
            }
            val originalTimeline = field.get(this) as? Timeline ?: return
            val originalKeyFrame = originalTimeline.keyFrames.firstOrNull() ?: return
            val originalHandler = originalKeyFrame.onFinished ?: return
            lateinit var patchedTimeline: Timeline
            patchedTimeline = Timeline(
                KeyFrame(
                    originalKeyFrame.time,
                    EventHandler { event ->
                        try {
                            originalHandler.handle(event)
                        } catch (e: ClassCastException) {
                            if(e.isWeakRedrawTimerSourceCast()) {
                                logger.warn(e) { "Stopping jeditermfx repaint timeline after source mismatch" }
                                patchedTimeline.stop()
                            } else {
                                throw e
                            }
                        }
                    }
                )
            ).apply {
                cycleCount = originalTimeline.cycleCount
            }
            originalTimeline.stop()
            field.set(this, patchedTimeline)
            patchedTimeline.play()
        }.onFailure { e ->
            logger.warn(e) { "Failed to patch jeditermfx repaint timeline" }
        }
    }

}

private fun ClassCastException.isWeakRedrawTimerSourceCast(): Boolean {
    val frame = stackTrace.firstOrNull()
    return message?.contains("javafx.animation.KeyFrame") == true
        && message?.contains("javafx.animation.Timeline") == true
        && frame?.className == "com.techsenger.jeditermfx.ui.TerminalPanel\$WeakRedrawTimer"
        && frame.methodName == "handle"
}

internal fun StageProperty.sanitizeTerminalStageProperty(): StageProperty {
    if(maximized && previousBoundary?.maximized == true && previousBoundary?.boundary?.isDefaultBoundary() == true) {
        maximized = false
        previousBoundary?.maximized = false
    }
    return this
}

private fun InsetProperty.isDefaultBoundary(): Boolean {
    return x == 100 && y == 100 && width == 500 && height == 600
}
