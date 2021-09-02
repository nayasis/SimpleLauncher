package com.github.nayasis.simplelauncher.view.terminal

import com.github.nayasis.kotlin.javafx.stage.addCloseRequest
import com.github.nayasis.simplelauncher.service.ConfigService
import javafx.stage.Stage
import tornadofx.App
import tornadofx.launch
import kotlin.system.exitProcess

fun main(args: Array<String>) {
    launch<TerminalTest>(args)
}

class TerminalTest: App() {
    override fun start(stage: Stage) {

        //        val terminal = Terminal(config).setCommand("cmd /c c: && cd \"c:\\Windows\" && dir").apply {this.stage = stage}

        val cd = "d:/download/test/chd"
        val terminal = Terminal("${cd}/chdman.exe createcd -f -i ${cd}/disc.cue -o ${cd}/disc.chd",cd)

//        val cd = "d:/download/test/cso"
//        val terminal = Terminal(config()).setCommand("${cd}/CisoPlus.exe -com -l9 ${cd}/disc.iso ${cd}/disc.cso",cd).apply {this.stage = stage}

        with(terminal) {
            title = "Terminal Test"
            width = 900.0
            height = 600.0
            addCloseRequest{
                ConfigService.save()
                exitProcess(0)
            }
            show()
        }

    }
}