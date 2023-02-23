package com.github.nayasis.simplelauncher.view.terminal.old

import com.github.nayasis.kotlin.basica.etc.error
import com.github.nayasis.kotlin.basica.exec.Command
import com.github.nayasis.kotlin.javafx.stage.addCloseRequest
import javafx.scene.Scene
import javafx.scene.paint.Color
import javafx.stage.Stage
import mu.KotlinLogging
import tornadofx.*

private val logger = KotlinLogging.logger {}

class TerminalOld(
    command: Command,
    onStart: ((TerminalOld) -> Unit)? = null,
    onDone: ((TerminalOld) -> Unit)? = null,
    onFail: ((Throwable, TerminalOld) -> Unit)? = null,
    onSuccess: ((TerminalOld) -> Unit)? = null,
    terminalConfig: TerminalConfigOld = TerminalConfigOld().apply {
        backgroundColor = Color.rgb(16, 16, 16).toHex()
        foregroundColor = Color.rgb(240, 240, 240).toHex()
        cursorColor = Color.rgb(255, 0, 0, 0.5).toHex()
        scrollbarVisible = false
        fontSize = 12
        scrollWhellMoveMultiplier = 3.0
        enableClipboardNotice = false
    },
): Stage() {

    private val terminal: TerminalPaneOld = TerminalPaneOld(
        command,
        {
            runLater { title = "Done - $title" }
            onDone?.invoke(this)
        },
        { e -> onFail?.invoke(e,this) },
        { onSuccess?.invoke(this) },
        terminalConfig,
    )

    init {
        scene  = Scene(terminal)
        title  = command.toString()
        width  = 700.0
        height = 600.0
        addCloseRequest {
            terminal.onDone = null
            terminal.onFail = null
            terminal.onSuccess = null
            terminal.webView.engine.load(null)
            runCatching {
                terminal.close()
            }.onFailure { logger.error(it) }
        }
        onStart?.invoke(this)
    }

}