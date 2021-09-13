package com.github.nayasis.simplelauncher.view.terminal

import com.github.nayasis.kotlin.javafx.property.StageProperty
import com.github.nayasis.kotlin.javafx.stage.Dialog
import com.github.nayasis.simplelauncher.service.ConfigService
import javafx.stage.Stage
import tornadofx.App
import tornadofx.launch
import tornadofx.runAsync
import tornadofx.runLater
import java.lang.Thread.sleep
import kotlin.system.exitProcess

fun main(args: Array<String>) {
    launch<TerminalTest>(args)
}

class TerminalTest: App() {
    override fun start(stage: Stage) {

        //        val terminal = Terminal(config).setCommand("cmd /c c: && cd \"c:\\Windows\" && dir").apply {this.stage = stage}

//        val cd = "d:/download/test/chd222"
        val cd = "d:/download/test/chd"
        val cmd = "${cd}/chdman.exe createcd -f -i ${cd}/disc.cue -o ${cd}/disc.chd"
//        val terminal = Terminal("${cd}/chdman.exe createcd -f -i ${cd}/disc.cue -o ${cd}/disc.chd",cd)

//        val cd = "d:/download/test/cso"
//        val terminal = Terminal(config()).setCommand("${cd}/CisoPlus.exe -com -l9 ${cd}/disc.iso ${cd}/disc.cso",cd).apply {this.stage = stage}

        val progress = Dialog.progress("Terminal test")

        val max = 2

        for( i in 1..max) {

            println("$i / $max")
            progress.updateMessage( "$i / $max")
            progress.updateProgress(i.toLong(),max.toLong())

            Terminal(cmd,onDone = {
                ConfigService.stageTerminal = StageProperty(it)
                runLater { it.close() }
            }).showAndWait()

        }

        progress.close()
        exitProcess(0)

    }
}