package com.github.nayasis.sample.stage

import com.github.nayasis.kotlin.javafx.stage.addMoveHandler
import com.github.nayasis.kotlin.javafx.stage.setBorderless
import javafx.geometry.Insets
import javafx.scene.layout.VBox
import javafx.stage.Stage
import javafx.stage.StageStyle
import tornadofx.*

fun main( args:Array<String> ) {
    launch<BorderlessApp>( args )
}

class BorderlessApp: App(BorderlessView::class) {

    override fun start(stage: Stage) {
        stage.initStyle(StageStyle.TRANSPARENT)
        super.start(stage)
    }
}

class BorderlessView: View() {

    override val root = vbox {
        prefWidth = 400.0
        prefHeight = 300.0
        hbox {
            id = "mainmenu"
            menubar {
                menu("File") {
                    menu("Connect") {
                        item("Facebook")
                        item("Twitter")
                    }
                    item("Save")
                    item("Quit") {
                        action { close() }
                    }
                }
                menu("Edit") {
                    item("Copy")
                    item("Paste")
                }
            }
        }
        textarea("content")
    }

    override fun onDock() {

        VBox.setMargin(root, Insets(5.0,5.0,5.0,5.0))

        with(currentStage!!) {
            setBorderless()
//            addMoveHandler(scene.lookup("#mainmenu"), buttonAll = true)
            addMoveHandler(scene.lookup("#mainmenu"), buttonClose = true)
        }
    }

}