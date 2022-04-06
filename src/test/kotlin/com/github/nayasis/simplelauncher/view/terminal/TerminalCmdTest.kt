package com.github.nayasis.simplelauncher.view.terminal

import com.github.nayasis.kotlin.basica.exec.Command
import javafx.stage.Stage
import tornadofx.App
import tornadofx.launch
import kotlin.system.exitProcess

fun main(args: Array<String>) {
    launch<TerminalCmdTest>(args)
}

class TerminalCmdTest: App() {
    override fun start(stage: Stage) {
        Terminal(Command("C:/app/SimpleLauncherApp/NSC Builder/NSCB.bat")).showAndWait()
//        Terminal(Command("cmd")).showAndWait()
        exitProcess(0)
    }
}