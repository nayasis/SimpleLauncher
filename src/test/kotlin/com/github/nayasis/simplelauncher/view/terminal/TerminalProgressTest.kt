package com.github.nayasis.simplelauncher.view.terminal

import com.github.nayasis.kotlin.javafx.stage.Dialog
import javafx.stage.Stage
import tornadofx.App
import tornadofx.launch

fun main(args: Array<String>) {
    launch<TerminalProgressTest>(args)
}

class TerminalProgressTest: App() {
    override fun start(stage: Stage) {

        val cd = "d:/download/test/chd"
        val cmd = "${cd}/chdman.exe createcd -f -i ${cd}/disc.cue -o ${cd}/disc.chd"

        val size = 2
        var i    = 1

        Dialog.progress {

            updateMessage( "${i} / ${size}")
            updateProgress(i.toLong(),size.toLong())

            Terminal(cmd,onDone = {
                tornadofx.runLater { it.close() }
            }).apply {
                title  = "test - ${i}"
            }.showAndWait()

        }

    }
}