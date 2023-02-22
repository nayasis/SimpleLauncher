package com.github.nayasis.simplelauncher.view.terminalfximp

import javafx.scene.Scene
import javafx.scene.layout.VBox
import javafx.scene.paint.Color
import javafx.stage.Stage
import tornadofx.App
import tornadofx.add
import tornadofx.launch
import tornadofx.runLater

fun main() {
    launch<TerminalAppStarter>()
}

class TerminalAppStarter: App() {

    override fun start(stage: Stage) {

        val darkConfig = TerminalConfig().apply {
            setBackgroundColor(Color.rgb(16, 16, 16))
            setForegroundColor(Color.rgb(240, 240, 240))
            setCursorColor(Color.rgb(255, 0, 0, 0.5))
        }


        val terminalView = Terminal(darkConfig)

        val root = VBox().apply {
            add(terminalView)
        }

        stage.scene = Scene(root)
        stage.show()

        runLater {
            terminalView.onTerminalReady()
        }


    }

}