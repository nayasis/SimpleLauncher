package com.github.nayasis.sample.modal

import com.github.nayasis.kotlin.javafx.stage.addMoveHandler
import com.github.nayasis.kotlin.javafx.stage.isBorderless
import com.github.nayasis.kotlin.javafx.stage.setBorderless
import javafx.beans.property.SimpleStringProperty
import javafx.geometry.Insets
import javafx.scene.control.MenuBar
import javafx.scene.layout.VBox
import javafx.stage.StageStyle
import tornadofx.*

class MyView: View() {

    val controller: MyController by inject<MyController>()

    val input = SimpleStringProperty()

    override val root = form {
        fieldset {
            field("Input") {
                textfield( input )
            }
            button("Commit") {
                action {
                    controller.writeToDb(input.value)
                    input.value = ""
                }
            }
            button("Open Internal Modal") {
                action {
                    openInternalWindow<Editor>()
                }
            }
            button("Open Modal") {
                action {
                    find<Editor>().openModal(stageStyle = StageStyle.TRANSPARENT)
                }
            }
            button("Open Window") {
                action {
                    find<Editor>().openWindow(stageStyle = StageStyle.TRANSPARENT)
                }
            }
        }
    }
}

class Editor: View() {

    lateinit var menubar: MenuBar

    override val root = vbox {
        menubar = menubar {}
        label("Editor")
        form {
            fieldset {
                field("First field")
                field("Second field")
            }
        }
        button("Save")
    }

    override fun onDock() {
        VBox.setMargin(root, Insets(5.0, 5.0, 5.0, 5.0))
        with(currentStage!!) {
            if(!isBorderless()) {
                setBorderless()
                addMoveHandler(menubar, buttonClose = true)
            }
        }
    }
}

// View     : Singleton
// Fragment : Instances

//class ModalEditor: Fragment() {
//    override val root = vbox {
//        label("Editor")
//        form {
//            fieldset {
//                field("First field")
//                field("Second field")
//            }
//        }
//        button("Save")
//    }
//}

class MyController: Controller() {
    fun writeToDb(value: String) {
        println( "writing $value to database!")
    }
}

class MyApp: App(MyView::class)

fun main(args: Array<String>) {
    launch<MyApp>(args)
}

