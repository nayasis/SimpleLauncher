package io.github.nayasis.terminal

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import io.github.nayasis.kotlin.basica.exec.Command
import io.github.nayasis.simplelauncher.view.Terminal
import javafx.application.Application
import javafx.application.Platform.runLater
import javafx.stage.Stage
import org.slf4j.LoggerFactory

fun main() {
    configureLogging()
    Application.launch(TerminalFxTest2::class.java)
}

private fun configureLogging() {
    listOf(
        "com.techsenger.jeditermfx.core",
        "com.techsenger.jeditermfx.ui",
        "com.techsenger.jeditermfx",
        "com.pty4j",
    ).forEach { packageName ->
        (LoggerFactory.getLogger(packageName) as Logger).level = Level.WARN
    }
}

class TerminalFxTest2: Application() {
    override fun start(stage: Stage) {
        val command = Command("cmd")
        val terminal = Terminal(
            command = command,
            onSuccess = { term ->
                println("Terminal started successfully.")
            },
            onFail = { term, error ->
                println("Error while running terminal: ${error.message}")
            },
            onAlways = { term ->
                println("Terminal closed.")
            }
        )
        terminal.show()
        runLater {
            terminal.sendCommand("dir")
        }
    }
}