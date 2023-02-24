package com.github.nayasis.simplelauncher.view.terminal.improve

import com.github.nayasis.kotlin.basica.etc.error
import com.github.nayasis.kotlin.basica.exec.Command
import com.github.nayasis.kotlin.javafx.property.StageProperty
import com.github.nayasis.simplelauncher.common.Context.Companion.config
import com.github.nayasis.terminalfx.kt.Terminal
import com.github.nayasis.terminalfx.kt.config.TerminalConfig
import com.github.nayasis.terminalfx.kt.config.toHex
import javafx.scene.Scene
import javafx.scene.paint.Color
import javafx.stage.Stage
import mu.KotlinLogging
import tornadofx.runLater

private val logger = KotlinLogging.logger {}

class TerminalNew(
    command: Command,
    var onDone: ((terminal: TerminalNew) -> Unit)? = null,
    var onFail: ((error: Throwable) -> Unit)? = null,
    var onSuccess: ((terminal: TerminalNew) -> Unit)? = null,
): Stage() {

    val terminal = Terminal(
        config = TerminalConfig().apply {
            cursorColor = "white"
            foregroundColor = Color.rgb(200, 200, 200).toHex()
            backgroundColor = Color.rgb(16, 16, 16).toHex()
            fontSize = 12
            scrollWheelMoveMultiplier = 3.0
            enableClipboardNotice = false
            scrollbarVisible = false
            size = config.terminalSize
        },
        command = command.command,
        workingDirectory = command.workingDirectory,
        onSuccess =  { _, _ ->
            onSuccess?.invoke(this)
        },
        onFail = { _, error ->
            onFail?.invoke(error)
        },
        onDone = {
            runLater {
                title = "Done - $title"
            }
            onDone?.invoke(this)
        },
    )

    init {

        scene  = Scene(terminal)
        title  = "$command"
        width  = 700.0
        height = 600.0

        setOnShown {
            config.stageTerminal?.bind(this)
        }
        setOnCloseRequest {
            config.stageTerminal = StageProperty(this)
            config.terminalSize = terminal.terminalSize
            terminal.onDone = null
            terminal.onFail = null
            terminal.onSuccess = null
            runCatching {
                terminal.close()
            }.onFailure { logger.error(it) }
        }

    }

}