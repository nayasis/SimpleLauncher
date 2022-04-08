package com.github.nayasis.simplelauncher.view.terminal

import com.github.nayasis.kotlin.basica.etc.error
import com.github.nayasis.kotlin.basica.exec.Command
import com.github.nayasis.kotlin.javafx.property.StageProperty
import com.github.nayasis.kotlin.javafx.stage.addCloseRequest
import com.github.nayasis.kotlin.javafx.stage.loadDefaultIcon
import com.github.nayasis.simplelauncher.service.ConfigService
import javafx.scene.Scene
import javafx.scene.paint.Color
import javafx.stage.Stage
import mu.KotlinLogging
import tornadofx.runLater

private val logger = KotlinLogging.logger {}

class Terminal(
    command: Command,
    onDone:((Terminal) -> Unit)? = null,
    onFail:((Throwable,Terminal) -> Unit)? = null,
    onSuccess:((Terminal) -> Unit)? = null,
    terminalConfig: TerminalConfig = TerminalConfig().apply {
        backgroundColor           = Color.rgb(16, 16, 16).toHex()
        foregroundColor           = Color.rgb(240, 240, 240).toHex()
        cursorColor               = Color.rgb(255, 0, 0, 0.5).toHex()
        scrollbarVisible          = false
        fontSize                  = 12
        scrollWhellMoveMultiplier = 3.0
        enableClipboardNotice     = false
    },
): Stage() {

    private val terminalPane = TerminalPane(
        command,
        {runLater {
//            title = "Done - $title"
            onDone?.invoke(this)
        }},
        { e -> onFail?.invoke(e,this) },
        { onSuccess?.invoke(this) },
        terminalConfig,
    )

    init {

        scene  = Scene(terminalPane)
        title  = command.toString()
        width  = 700.0
        height = 600.0

        addCloseRequest {
            ConfigService.stageTerminal = StageProperty(this)
            terminalPane.onDone    = null
            terminalPane.onFail    = null
            terminalPane.onSuccess = null
            runCatching {
                terminalPane.close()
            }.onFailure { logger.error(it) }
        }

        ConfigService.stageTerminal?.bind(this)
        loadDefaultIcon()

    }

}