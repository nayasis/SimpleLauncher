package com.github.nayasis.kotlin.javafx.stage

import javafx.application.Application
import javafx.scene.text.Font
import mu.KotlinLogging
import tornadofx.App
import tornadofx.Stylesheet
import tornadofx.View
import tornadofx.action
import tornadofx.button
import tornadofx.launch
import tornadofx.vbox
import java.lang.Thread.sleep

private val logger = KotlinLogging.logger {}

fun main(args: Array<String>) {
    launch<DialogTest>(args)
}

class DialogTest: App(DialogTestView::class,MyStylesheet::class)

class DialogTestView: View("dialog test") {

    override val root = vbox {
        button("error") {action {
            Dialog.error("test error (1234567890)", RuntimeException("test"))
        }}
        button("confirm") {action {
            if( Dialog.confirm("is it ok (1234567890) ??") ) {
                Dialog.alert("it is ok !!")
            }
        }}
        button("progress") {action {
            Dialog.progress("header") {
                val max = 40
                for (i in 1..max) {
                    println( "$i to $max" )
                    updateProgress(i.toLong(), max.toLong())
                    updateMessage("$i / $max")
                    updateTitle("title : $i")
                    sleep(100)
                }
            }
        }}
        button("progress manually") { action {
            val progress = Dialog.progress("Terminal test")
            runAsync {
                val max = 40
                for (i in 1..max) {
                    println("$i / $max")
                    progress.updateMessage("$i / $max")
                    progress.updateProgress(i.toLong(), max.toLong())
                    sleep(100)
                }
                progress.close()
            }
        }}
        prefWidth = 300.0
        prefHeight = 200.0
    }

    init {
        val userAgentStylesheet = Application.getUserAgentStylesheet()

        logger.debug {
            "style sheet : ${Application.getUserAgentStylesheet()}"
            "font : ${Font.getDefault().name}"
        }

    }

}

// set default font
class MyStylesheet : Stylesheet() {
    init {
        root {
            fontFamily = "Arial"
        }
    }
}