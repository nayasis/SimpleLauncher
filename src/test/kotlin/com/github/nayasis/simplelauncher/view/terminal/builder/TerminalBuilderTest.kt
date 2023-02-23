package com.github.nayasis.simplelauncher.view.terminal.builder

import com.github.nayasis.kotlin.basica.etc.Platforms
import com.github.nayasis.terminalfx.kt.Terminal
import com.github.nayasis.terminalfx.kt.config.TerminalConfig
import com.github.nayasis.terminalfx.kt.config.toHex
import javafx.application.Application
import javafx.application.Platform
import javafx.scene.Scene
import javafx.scene.paint.Color
import javafx.stage.Stage
import mu.KotlinLogging
import tornadofx.launch
import kotlin.system.exitProcess

private val logger = KotlinLogging.logger {}

fun main() {
    launch<TerminalBuilderTest>()
}

class TerminalBuilderTest: Application() {
    override fun start(stage: Stage?) {

        val config = TerminalConfig().apply {
            cursorColor = "white"
            foregroundColor = Color.rgb(200, 200, 200).toHex()
            backgroundColor = Color.rgb(16, 16, 16).toHex()
            fontSize = 12
            scrollWheelMoveMultiplier = 3.0
            enableClipboardNotice = false
            scrollbarVisible = false
        }

        val terminal = Terminal(
            config = config,
            command = (if(Platforms.isWindows) "cmd.exe" else "/bin/bash -i").split("\\s+"),
            onSuccess = { terminal, exitValue ->
                logger.debug { """
                    command    : ${terminal.command}
                    exit value : $exitValue
                """.trimIndent() }
            },
            onDone = {
                stage?.close()
            }
        )

        stage?.apply {
            title  = "TerminalFX"
            width  = 600.0
            height = 400.0
            scene  = Scene(terminal)
            show()
        }

        terminal.onTerminalFxReady { terminal.command("dir\r") }

    }
    override fun stop() {
        Platform.exit()
        exitProcess(0)
    }
}