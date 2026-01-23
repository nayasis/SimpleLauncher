package io.github.nayasis.terminal

import io.github.nayasis.kotlin.basica.exec.Command
import io.github.nayasis.simplelauncher.view.Terminal
import javafx.application.Application
import javafx.stage.Stage

fun main() {
    configureLogging()
    Application.launch(TerminalFxTestSimple::class.java)
}

class TerminalFxTestSimple: Application() {
    override fun start(stage: Stage) {
        val command = Command("src/test/resources/test-program/test.exe 5")
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