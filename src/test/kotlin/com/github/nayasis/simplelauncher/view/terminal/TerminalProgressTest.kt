package com.github.nayasis.simplelauncher.view.terminal

import com.github.nayasis.kotlin.basica.exec.Command
import com.github.nayasis.kotlin.javafx.property.StageProperty
import com.github.nayasis.kotlin.javafx.stage.Dialog
import com.github.nayasis.simplelauncher.service.ConfigService
import javafx.stage.Modality
import javafx.stage.Stage
import tornadofx.App
import tornadofx.launch
import tornadofx.runLater
import kotlin.system.exitProcess

fun main(args: Array<String>) {
    launch<TerminalProgressTest>(args)
}

class TerminalProgressTest: App() {
    override fun start(stage: Stage) {

        val cd = "d:/download/test/chd"
        val cmd = "${cd}/chdman1.exe createcd -f -i ${cd}/disc1.cue -o ${cd}/disc1.chd"

        val progress = Dialog.progress("Terminal test").apply {
            initModality(Modality.NONE)
        }
        val max = 2

        for( i in 1..max) {

            println("$i / $max")
            progress.updateMessage( "$i / $max")
            progress.updateProgress(i.toLong(),max.toLong())

            Terminal(
                Command(cmd),
                onSuccess = {
                    runLater { it.close() }
                },
                onFail = { throwable, it ->
                    runLater {
                        Dialog.error(throwable)
                        it.close()
                    }
                },
                onDone = {
                    ConfigService.stageTerminal = StageProperty(it)
                }
            ).showAndWait()

        }

        progress.close()

        runLater {
            exitProcess(0)
        }

    }
}