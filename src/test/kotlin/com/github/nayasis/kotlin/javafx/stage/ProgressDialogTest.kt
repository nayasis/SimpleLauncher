package com.github.nayasis.kotlin.javafx.stage

import javafx.concurrent.Task
import javafx.stage.Stage
import mu.KotlinLogging
import tornadofx.App
import tornadofx.FXTask
import tornadofx.launch
import tornadofx.runAsync
import java.lang.Thread.sleep

private val logger = KotlinLogging.logger {}

fun main(args: Array<String>) {
    launch<ProgressDialogTest>(args)
}

class ProgressDialogTest: App() {
    override fun start(stage: Stage) {

        val task = object: Task<Void?>() {
            val max = 100
            override fun call(): Void? {
                for( i in 1..max) {
                    logger.debug { "$i to $max" }
                    updateProgress(i.toLong(),max.toLong())
                    sleep(100)
                }
                return null
            }
        }

//        stage.show()

        val dialog = ProgressDialog(task)
        dialog.show()

        task.run()
    }

}