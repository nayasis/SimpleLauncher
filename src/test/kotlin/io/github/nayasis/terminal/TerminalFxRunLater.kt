package io.github.nayasis.terminal

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import io.github.nayasis.kotlin.basica.exec.Command
import io.github.nayasis.simplelauncher.view.Terminal
import io.github.oshai.kotlinlogging.KotlinLogging
import javafx.application.Application
import javafx.stage.Stage
import org.slf4j.LoggerFactory
import tornadofx.runLater
import tornadofx.seconds

private val logger = KotlinLogging.logger {}

fun main() {
    configureLogging()
    Application.launch(TerminalFxRunLater::class.java)
}

fun configureLogging() {
    listOf(
        "com.techsenger.jeditermfx.core",
        "com.techsenger.jeditermfx.ui",
        "com.techsenger.jeditermfx",
        "com.pty4j",
    ).forEach { packageName ->
        (LoggerFactory.getLogger(packageName) as Logger).level = Level.WARN
    }
}

class TerminalFxRunLater: Application() {
    override fun start(stage: Stage) {
        val command = Command("cmd")
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
        runLater(1.seconds) {
            terminal.sendCommand("dir")
        }
        terminal.show()
    }
}