package com.github.nayasis.simplelauncher.view.terminal

import javafx.scene.Scene
import javafx.scene.paint.Color
import javafx.stage.Stage
import tornadofx.App
import tornadofx.launch
import kotlin.system.exitProcess

fun main(args: Array<String>) {
    launch<TerminalTest>(args)
}

class TerminalTest: App() {
    override fun start(stage: Stage) {

        val config = TerminalConfig().apply {
            backgroundColor = Color.rgb(16, 16, 16).toHex()
            foregroundColor = Color.rgb(240, 240, 240).toHex()
            cursorColor = Color.rgb(255, 0, 0, 0.5).toHex()
            scrollbarVisible = false
            fontSize = 12
            scrollWhellMoveMultiplier = 3.0
            enableClipboardNotice = false
        }

        val terminal = Terminal(config).setCommand("cmd /c c: && cd \"c:\\Windows\" && dir").apply {this.stage = stage}

        stage.title = "Terminal Test"
        stage.scene = Scene(terminal, 900.0, 600.0)
        stage.show()

        stage.setOnCloseRequest {
            stage.close()
            exitProcess(0)
        }

    }
}