package com.github.nayasis.kotlin.javafx.stage

import javafx.stage.Stage
import tornadofx.App
import tornadofx.FXTask
import tornadofx.launch
import tornadofx.runAsync
import java.lang.Thread.sleep
import kotlin.system.exitProcess

fun main(args: Array<String>) {
    launch<ProgressDialogTest>(args)
}

class ProgressDialogTest: App() {
    override fun start(stage: Stage) {

        val task = FXTask {
            val max = 40
            for (i in 1..max) {
                println( "$i to $max" )
                updateProgress(i.toLong(), max.toLong())
                updateMessage("$i / $max")
                updateTitle("title : $i")
                sleep(100)
            }
        }

        ProgressDialog(task).apply {
            headerText = "header"
            contentText = "content"
        }

        runAsync {
            task.run()
            exitProcess(0)
        }

    }
}