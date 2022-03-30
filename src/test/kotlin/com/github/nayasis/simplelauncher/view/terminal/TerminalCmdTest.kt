package com.github.nayasis.simplelauncher.view.terminal

import com.github.nayasis.kotlin.basica.exec.Command
import com.github.nayasis.kotlin.javafx.property.StageProperty
import com.github.nayasis.kotlin.javafx.stage.Dialog
import com.github.nayasis.simplelauncher.service.ConfigService
import javafx.stage.Stage
import tornadofx.App
import tornadofx.launch
import tornadofx.runLater
import kotlin.system.exitProcess

fun main(args: Array<String>) {
    launch<TerminalCmdTest>(args)
}

class TerminalCmdTest: App() {
    override fun start(stage: Stage) {
        Terminal(Command("C:/app/SimpleLauncherApp/NSC Builder/NSCB.exe")).showAndWait()
        exitProcess(0)
    }
}