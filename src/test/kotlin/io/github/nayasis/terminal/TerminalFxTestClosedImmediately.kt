package io.github.nayasis.terminal

import io.github.nayasis.kotlin.basica.exec.Command
import io.github.nayasis.simplelauncher.view.Terminal
import io.github.oshai.kotlinlogging.KotlinLogging
import javafx.application.Application
import javafx.stage.Stage

private val logger = KotlinLogging.logger {}

fun main() {
    configureLogging()
    Application.launch(TerminalFxTestClosedImmediately::class.java)
}

class TerminalFxTestClosedImmediately: Application() {
    override fun start(stage: Stage) {
        val command = Command("src/test/resources/test-program/test.exe zero 0")
        val terminal = Terminal(
            command = command,
            onSuccess = { term ->
                println("Terminal started successfully.")
            },
            onFail = { error ->
                println("Error while running terminal: ${error.message}")
            },
            onAlways = { term ->
                println("Terminal closed.")
            }
        )
        terminal.show()
    }
}