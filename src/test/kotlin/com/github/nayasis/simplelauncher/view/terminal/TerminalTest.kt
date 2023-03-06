package com.github.nayasis.simplelauncher.view.terminal

import com.github.nayasis.kotlin.basica.exec.Command
import com.github.nayasis.simplelauncher.view.terminal.old.TerminalOld
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

//        val cd = "d:/download/test/chd222"
        val cd = "d:/download/test/chd"
        val cmd = "${cd}/chdman.exe createcd -f -i ${cd}/disc1.cue -o ${cd}/disc1.chd"
//        val terminal = Terminal("${cd}/chdman.exe createcd -f -i ${cd}/disc.cue -o ${cd}/disc.chd",cd)

//        val cd = "d:/download/test/cso"
//        val terminal = Terminal(config()).setCommand("${cd}/CisoPlus.exe -com -l9 ${cd}/disc.iso ${cd}/disc.cso",cd).apply {this.stage = stage}

//        Terminal(Command(cmd)).showAndWait()
//        Terminal(Command("cmd")).showAndWait()
//        Terminal(Command("C:/app/SimpleLauncherApp/NSC Builder/NSCB.exe")).showAndWait()
        TerminalOld(Command("cmd").apply { workingDirectory = "C:/app/SimpleLauncherApp/NSC Builder" }).showAndWait()

        exitProcess(0)

    }
}