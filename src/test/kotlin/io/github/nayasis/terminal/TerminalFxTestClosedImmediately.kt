package io.github.nayasis.terminal

import io.github.nayasis.kotlin.basica.exec.Command
import io.github.nayasis.simplelauncher.view.Terminal
import javafx.application.Application
import javafx.stage.Stage

fun main() {
    Application.launch(TerminalFxTestClosedImmediately::class.java)
}

class TerminalFxTestClosedImmediately: Application() {
    override fun start(stage: Stage) {
        val command = Command("src/test/resources/test-program/test.exe zero 0")
        val terminal = Terminal(
            onSuccess = {
                println("Terminal started successfully.")
            },
            onFail = { error ->
                println("Error while running terminal: ${error.message}")
            },
            onAlways = {
                println("Terminal closed.")
            }
        )
        terminal.show()
        terminal.run(command)
    }
}