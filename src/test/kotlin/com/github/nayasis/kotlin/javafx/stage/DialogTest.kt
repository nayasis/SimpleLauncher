package com.github.nayasis.kotlin.javafx.stage

import tornadofx.App
import tornadofx.View
import tornadofx.action
import tornadofx.button
import tornadofx.launch
import tornadofx.vbox

fun main(args: Array<String>) {
    launch<DialogTest>(args)
}

class DialogTest: App(DialogTestView::class)

class DialogTestView: View("dialog test") {

    override val root = vbox {
        button("error") {
           action {
               Dialog.error("test error", RuntimeException("test"))
           }
        }
        button("confirm") {
            action {
                if( Dialog.confirm("is it ok ??") ) {
                    Dialog.alert("it is ok !!")
                }
            }
        }
    }

}