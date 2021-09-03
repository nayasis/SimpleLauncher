package com.github.nayasis.simplelauncher.view.terminal

import com.github.nayasis.kotlin.basica.etc.error
import com.github.nayasis.kotlin.javafx.property.StageProperty
import com.github.nayasis.kotlin.javafx.stage.addCloseRequest
import com.github.nayasis.simplelauncher.service.ConfigService
import javafx.scene.Scene
import javafx.scene.paint.Color
import javafx.stage.Stage
import mu.KotlinLogging
import org.apache.commons.exec.ShutdownHookProcessDestroyer

private val logger = KotlinLogging.logger {}

class Terminal(
    command: String,
    workingDirectory: String? = null,
    postAction:(() -> Unit)? = null,
    terminalConfig: TerminalConfig = TerminalConfig().apply {
        backgroundColor = Color.rgb(16, 16, 16).toHex()
        foregroundColor = Color.rgb(240, 240, 240).toHex()
        cursorColor = Color.rgb(255, 0, 0, 0.5).toHex()
        scrollbarVisible = false
        fontSize = 12
        scrollWhellMoveMultiplier = 3.0
        enableClipboardNotice = false
    },
): Stage() {

    private val terminal: TerminalPane = TerminalPane(
        command,
        workingDirectory,
        postAction,
        terminalConfig,
    )

    init {
        scene = Scene(terminal)
        addCloseRequest {
            ConfigService.stageTerminal = StageProperty(this)
            terminal.postAction = null
            terminal.webView.engine.load(null)
            runCatching {
                terminal.destory()
            }.onFailure { logger.error(it) }
        }
        ConfigService.stageTerminal?.let {
            it.bind(this)
        }
    }

}