package com.github.nayasis.kotlin.javafx.stage

import com.github.nayasis.kotlin.basica.core.path.div
import com.github.nayasis.kotlin.basica.core.path.userHome
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
import javafx.stage.FileChooser.ExtensionFilter
import javafx.stage.Modality.WINDOW_MODAL
import javafx.stage.Stage
import javafx.stage.Window
import mu.KotlinLogging
import tornadofx.*
import tornadofx.FileChooserMode.*
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
            initOwner(Stages.focusedWindow)
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
            initOwner(Stages.focusedWindow)
            with( dialogPane.scene.window as Stage ) {
                isAlwaysOnTop = true
                loadDefaultIcon()
            }
        }.showAndWait().get()
    }

    fun progress(title: String? = null, func: (FXTask<*>.() -> Any)? = null): ProgressDialog {
        val task = func?.let {FXTask(func=it)}
        val dialog = ProgressDialog(task)
        dialog.title = title ?: " "
        dialog.initOwner(Stages.focusedWindow)
        if( task == null ) {
            runLater { dialog.show() }
        } else {
            runAsync { task.run() }
        }
        return dialog
    }

    fun filePicker(title: String = "", extension: String = "", description: String = "", initialDirectory: File? = null, mode: FileChooserMode = Single, owner: Window? = null, option: FileChooser.() -> Unit = {}): List<File> {
        val chooser = FileChooser().apply {
            this.title = title
            this.extensionFilters.add( ExtensionFilter(description.ifEmpty{extension}, extension.split(",;")) )
            this.initialDirectory = if( initialDirectory != null && initialDirectory.exists() && initialDirectory.canRead() ) {
                initialDirectory
            } else {
                dirDesktop()
            }
        }
        option(chooser)
        return when(mode) {
            Single -> {
                val result = chooser.showOpenDialog(owner)
                if (result == null) emptyList() else listOf(result)
            }
            Multi -> chooser.showOpenMultipleDialog(owner) ?: emptyList()
            Save -> {
                val result = chooser.showSaveDialog(owner)
                if (result == null) emptyList() else listOf(result)
            }
            else -> emptyList()
        }


    }

    fun dirPicker(title: String = "", initialDirectory: File? = null, owner: Window? = null, option: DirectoryChooser.() -> Unit = {}): File? {
        val chooser = DirectoryChooser().apply {
            this.title = title
            this.initialDirectory = if( initialDirectory != null && initialDirectory.exists() && initialDirectory.canRead() ) {
                initialDirectory
            } else {
                dirDesktop()
            }
        }
        option(chooser)
        return chooser.showDialog(owner)
    }

    private fun dirDesktop(): File {
        return userHome().let {
            when{
                Platforms.isWindows -> it / "Desktop"
                else -> it
            }
        }.toFile()
    }

}}