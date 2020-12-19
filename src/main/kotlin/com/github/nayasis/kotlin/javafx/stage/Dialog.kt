package com.github.nayasis.kotlin.javafx.stage

import com.github.nayasis.kotlin.basica.exception.Caller
import com.github.nayasis.kotlin.basica.message
import javafx.scene.control.*
import javafx.scene.control.Alert.AlertType
import javafx.scene.input.KeyCode
import javafx.scene.input.KeyEvent
import javafx.scene.layout.GridPane
import javafx.scene.layout.Priority.ALWAYS
import javafx.stage.Modality.WINDOW_MODAL
import javafx.stage.Stage
import mu.KotlinLogging
import tornadofx.*
import kotlin.Double.Companion.MAX_VALUE

private val logger = KotlinLogging.logger {  }

fun dialog(type: AlertType, message: String): Alert {
    return Alert(type).apply {
        title = null
        headerText = null
        contentText = message
        initModality(WINDOW_MODAL)
        initOwner(focusedWindow())
        with( dialogPane.scene.window as Stage ) {
            isAlwaysOnTop = true
            loadDefaultIcon()
        }
    }
}

fun alert(message: String) {
    dialog(AlertType.INFORMATION, message).showAndWait()
}

@Suppress("MoveLambdaOutsideParentheses")
fun confirm(message: String): Boolean {

    val yes  = ButtonType("Yes".message())
    val no   = ButtonType("No".message())

    return dialog(AlertType.CONFIRMATION, message).apply {
        buttonTypes.addAll(yes,no)
        listOf(yes,no).map{ dialogPane.lookupButton(it) }.forEach {
            it.addEventHandler(KeyEvent.KEY_PRESSED, { e ->
                if( e.code == KeyCode.ENTER && e.target is Button )
                    (e.target as Button).fire()
            })}
    }.showAndWait().get() == yes

}

fun error(message: String, exception: Throwable? = null) {
    dialog(AlertType.ERROR, message).apply {
        if( exception != null ) {
            logger.error {
                Caller(4).let { ">> called (${it.fileName}:${it.lineNo})" }
            }
            logger.error( exception.message, exception )
            dialogPane.expandableContent = GridPane().apply {
                maxWidth = MAX_VALUE
                add( TextArea(exception.stackTraceToString()).apply{
                    isEditable = false
                    maxWidth   = MAX_VALUE
                    maxHeight  = MAX_VALUE
                    vgrow      = ALWAYS
                    hgrow      = ALWAYS
                }, 0, 0 )
            }
        }
    }.showAndWait()
}

fun prompt(message: String): String {
    return TextInputDialog().apply {
        headerText = message
        initModality(WINDOW_MODAL)
        initOwner(focusedWindow())
        with( dialogPane.scene.window as Stage ) {
            isAlwaysOnTop = true
            loadDefaultIcon()
        }
    }.showAndWait().get()
}

fun filePicker(title: String, extensions: String) {

}