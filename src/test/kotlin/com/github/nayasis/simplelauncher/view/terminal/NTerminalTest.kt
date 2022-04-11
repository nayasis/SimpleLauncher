package com.github.nayasis.simplelauncher.view.terminal

import com.github.nayasis.kotlin.basica.etc.error
import com.github.nayasis.kotlin.basica.exec.Command
import com.github.nayasis.kotlin.javafx.stage.addCloseRequest
import com.pty4j.PtyProcessBuilder
import javafx.scene.Scene
import javafx.stage.Stage
import mu.KotlinLogging
import tornadofx.App
import tornadofx.launch
import kotlin.system.exitProcess

private val logger = KotlinLogging.logger {}

fun main(args: Array<String>) {
    launch<NTerminalTest>(args)
}

class NTerminalTest: App() {
    override fun start(stage: Stage) {
//        val process = ProcessBuilder("cmd.exe").start()
        val process = PtyProcessBuilder(arrayOf("cmd.exe")).start()
//        val command = Command("cmd").run(false)
        NTerminalStage(process).showAndWait()
        exitProcess(0)
    }
}

class NTerminalStage(
    process: Process
): Stage() {

    private val terminal = NTerminalPane(process)

    init {
        scene  = Scene(terminal)
        title  = "test"
        width  = 700.0
        height = 600.0
        addCloseRequest {
            try {
                terminal.close()
            } catch (e: Exception) {
                logger.error(e)
            }
        }
    }

}