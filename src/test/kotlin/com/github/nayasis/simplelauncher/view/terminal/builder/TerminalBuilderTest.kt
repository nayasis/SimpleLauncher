package com.github.nayasis.simplelauncher.view.terminal.builder

import javafx.stage.Stage
import jdk.internal.org.jline.terminal.TerminalBuilder
import tornadofx.App
import tornadofx.View
import tornadofx.launch
import tornadofx.tabpane
import kotlin.system.exitProcess


fun main(args: Array<String>) {
//    launch<TerminalBuilderTest>(args)
}

//class TerminalBuilderTest: App() {
//    override fun start(stage: Stage) {
//        Terminal(Command("cmd")).showAndWait()
//        exitProcess(0)
//    }
//}
//
//class TerminalBuilderView: View("test") {
//
//
//
//    override val root = tabpane {
//        add(
//
//        )
//    }
//
//    private fun config() {
//        val darkConfig = TerminalConfig()
//        darkConfig.backgroundColor = Color.rgb(16, 16, 16)
//        darkConfig.foregroundColor = Color.rgb(240, 240, 240)
//        darkConfig.cursorColor = Color.rgb(255, 0, 0, 0.5)
//
//        val terminalBuilder = TerminalBuilder(darkConfig)
//        val terminal: TerminalTab = terminalBuilder.newTerminal()
//    }
//
//}