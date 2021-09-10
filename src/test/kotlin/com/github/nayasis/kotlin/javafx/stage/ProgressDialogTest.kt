package com.github.nayasis.kotlin.javafx.stage

import javafx.stage.Stage
import mu.KotlinLogging
import tornadofx.App
import tornadofx.FXTask
import tornadofx.launch
import tornadofx.runAsync
import tornadofx.runLater
import java.lang.Thread.sleep
import kotlin.system.exitProcess

private val logger = KotlinLogging.logger {}

fun main(args: Array<String>) {
    launch<ProgressDialogTest>(args)
}

class ProgressDialogTest: App() {
    override fun start(stage: Stage) {

//        val fxtask = FXTask {
//            val max = 100
//            for (i in 1..max) {
//                logger.debug { "$i to $max" }
//                updateProgress(i.toLong(), max.toLong())
//                updateMessage("$i / $max")
//                updateTitle("title : $i")
//                sleep(100)
//            }
//        }
//
//        val dialog = ProgressDialog(fxtask)
//        dialog.headerText = "header"
//        dialog.contentText = "content"
//        dialog.show()
//
//        runAsync {
//            fxtask.run()
//            exitProcess(0)
//        }

//        val dialog = Dialog.progress {
//            val max = 100
//            for (i in 1..max) {
//                logger.debug { "$i to $max" }
//                updateProgress(i.toLong(), max.toLong())
//                updateMessage("$i / $max")
//                updateTitle("title : $i")
//                sleep(100)
//            }
//            exitProcess(0)
//        }

        val progress = Dialog.progress("Terminal test")

        runAsync {
            val max = 20
            for( i in 1..max) {
                println("$i / 10")
                progress.updateMessage( "$i / $max")
                progress.updateProgress(i.toLong(),max.toLong())
                sleep(200)
            }
            progress.closeForcibly()
            runLater { exitProcess(0) }
        }

    }
}