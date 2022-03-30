package com.github.nayasis.simplelauncher.view.terminal

import com.github.nayasis.kotlin.basica.exec.Command
import com.github.nayasis.kotlin.basica.exec.CommandExecutor
import com.github.nayasis.kotlin.javafx.property.StageProperty
import com.github.nayasis.kotlin.javafx.stage.Dialog
import com.github.nayasis.simplelauncher.service.ConfigService
import javafx.stage.Stage
import tornadofx.App
import tornadofx.launch
import tornadofx.runLater
import kotlin.system.exitProcess

fun main(args: Array<String>) {
    launch<ExecutorTest>(args)
}

class ExecutorTest: App() {
    override fun start(stage: Stage) {

        val cd = "d:\\download\\test\\chd"
        val cmds = listOf(
            "${cd}\\chdman.exe createcd -f -i ${cd}\\disc1.cue -o ${cd}\\disc1.chd",
            "${cd}\\chdman.exe createcd -f -i ${cd}\\disc2.cue -o ${cd}\\disc2.chd",
        )

//        cmds.forEachIndexed { index, cmd ->
//            println(">> $cmd")
//            val executor = CommandExecutor().apply {
//                onProcessFailed = { e -> Dialog.error(e) }
//            }.run(cmd)
//            executor.waitFor()
//        }
//        println(">> Done")

        Dialog.progress("test") {
            cmds.forEachIndexed { index, cmd ->
                println(">> start :$cmd")
                updateMessage("$cmd")
                updateProgress(index + 1L, cmds.size.toLong())
                val executor = CommandExecutor().apply {
                    onProcessFailed = { e -> Dialog.error(e) }
                }.runOnSystemOut(Command(cmd))
                executor.waitFor()
                println(">> end : $cmd")
            }
            println(">> Done")
            exitProcess(0)
        }

    }
}