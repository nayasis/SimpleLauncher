package com.github.nayasis.kotlin.javafx.stage

import com.github.nayasis.kotlin.basica.core.path.div
import com.github.nayasis.kotlin.basica.core.path.userHome
import com.github.nayasis.kotlin.basica.core.string.message
import com.github.nayasis.kotlin.basica.core.string.toFile
import com.github.nayasis.kotlin.basica.core.validator.nvl
import com.github.nayasis.kotlin.basica.etc.Platforms
import com.github.nayasis.kotlin.basica.etc.error
import javafx.scene.control.*
import javafx.scene.control.Alert.AlertType
import javafx.scene.input.KeyCode
import javafx.scene.input.KeyEvent
import javafx.scene.layout.GridPane
import javafx.scene.layout.Priority.ALWAYS
import javafx.stage.DirectoryChooser
import javafx.stage.FileChooser
import javafx.stage.Modality.WINDOW_MODAL
import javafx.stage.Stage
import mu.KotlinLogging
import java.io.File
import kotlin.Double.Companion.MAX_VALUE

private val logger = KotlinLogging.logger {}

class Dialog { companion object {

    fun dialog(type: AlertType, message: String?): Alert {
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

    fun alert(message: String?) {
        dialog(AlertType.INFORMATION, message).showAndWait()
    }

    fun confirm(message: String?): Boolean {
        return dialog(AlertType.CONFIRMATION, message).apply {
            buttonTypes.map { dialogPane.lookupButton(it) }.forEach {
                it.addEventHandler(KeyEvent.KEY_PRESSED) { e ->
                    if (e.code == KeyCode.ENTER && e.target is Button)
                        (e.target as Button).fire()
                }
            }
        }.showAndWait().get() == ButtonType.OK
    }

    fun error(message: String?, exception: Throwable? = null) {
        dialog(AlertType.ERROR, message ?: exception?.message).apply {
            if( exception != null ) {
                logger.error(exception)
                dialogPane.expandableContent = GridPane().apply {
                    maxWidth = MAX_VALUE
                    add( TextArea(exception.stackTraceToString()).apply{
                        isEditable = false
                        maxWidth   = MAX_VALUE
                        maxHeight  = MAX_VALUE
                        GridPane.setVgrow(this,ALWAYS)
                        GridPane.setHgrow(this,ALWAYS)
                    }, 0, 0 )
                }
            }
        }.showAndWait()
    }

    fun error(exception: Throwable?) {
        error(exception?.message, exception)
    }

    fun prompt(message: String?): String {
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

    fun filePicker(title: String = "", extensions: String = "", extensionDescription: String = "", initialDirectory: File?): FileChooser {
        return FileChooser().apply {
            this.title = title
            this.extensionFilters.add( FileChooser.ExtensionFilter(nvl(extensionDescription,extensions), extensions.split(",")) )
            if( initialDirectory != null && initialDirectory.exists() && initialDirectory.canRead() ) {
                this.initialDirectory = initialDirectory
            } else {
                this.initialDirectory = dirDesktop()
            }
        }
    }

    fun filePicker(title: String = "", extensions: String = "", extensionDescription: String = "", initialDirectory: String? = null): FileChooser =
        filePicker(title,extensions, extensionDescription,initialDirectory?.toFile())

    fun dirPicker(title: String = "", initialDirectory: File?): DirectoryChooser {
        return DirectoryChooser().apply {
            this.title = title
            if( initialDirectory != null && initialDirectory.exists() && initialDirectory.canRead() ) {
                this.initialDirectory = initialDirectory
            } else {
                this.initialDirectory = dirDesktop()
            }
        }
    }

    fun dirPicker(title: String = "", initialDirectory: String? = null): DirectoryChooser =
        dirPicker(title, initialDirectory?.toFile())

    private fun dirDesktop(): File {
        return userHome().let {
            when{
                Platforms.isWindows -> it / "Desktop"
                else -> it
            }
        }.toFile()
    }

}}