package com.github.nayasis.kotlin.javafx.stage

import com.github.nayasis.kotlin.basica.model.Messages
import javafx.application.Platform
import javafx.beans.value.ChangeListener
import javafx.concurrent.Task
import javafx.concurrent.Worker
import javafx.concurrent.Worker.State.*
import javafx.geometry.Insets
import javafx.scene.control.ButtonBar
import javafx.scene.control.ButtonType
import javafx.scene.control.Dialog
import javafx.scene.control.Label
import javafx.scene.control.ProgressBar
import javafx.scene.layout.Region
import javafx.scene.layout.VBox
import javafx.scene.paint.Color
import javafx.stage.Stage
import javafx.stage.StageStyle
import javafx.util.Callback
import java.util.concurrent.Executors

open class ProgressDialog: Dialog<Void> {

    constructor(worker: Worker<*>) {

        if( ! isValid(worker) ) return

        val scene = dialogPane.scene.apply {
            fill = Color.TRANSPARENT
            stylesheets.add("/view/dialog-progress.css")
        }
        val stage = (scene.window as Stage).apply {
            initStyle(StageStyle.TRANSPARENT)
            isAlwaysOnTop = true
            loadDefaultIcon()
            addMoveHandler(dialogPane)
        }

        // set style
        resultConverter = Callback { null }

        // set view
        val progressMessage = Label()

        val content = WorkerProgressPane(this).apply{
            maxWidth = Double.MAX_VALUE
            setWorker(worker)
        }

        dialogPane.content = VBox(10.0, progressMessage, content).apply {
            maxWidth = Double.MAX_VALUE
            setPrefSize(400.0, 40.0)
        }

        // bind text
        worker.titleProperty().addListener { _, _, new: String? -> dialogPane.headerText = Messages[new] }
        worker.messageProperty().addListener { _, _, new: String? -> progressMessage.text = Messages[new] }

    }

    private fun isValid(worker: Worker<*>): Boolean {
        return when (worker.state) {
            CANCELLED, FAILED, SUCCEEDED -> false
            else -> true
        }
    }

    companion object {
        fun run(title:String?, task: Task<*>) {
            if( ! Platform.isFxApplicationThread() ) return
            val dialog = ProgressDialog(task).apply {
                this.title = title
            }
            dialog.show()
            Executors.newSingleThreadExecutor().execute(task)
        }
    }

}

open class WorkerProgressPane: Region {

    var localWorker: Worker<*>? = null

    var dialogVisible = false
    var cancelDialogShow = false

    val stateListener = ChangeListener<Worker.State> { _, old, new ->
        when (new) {
            CANCELLED, FAILED, SUCCEEDED -> if (!dialogVisible) {
                cancelDialogShow = true
                end()
            } else if (old == SCHEDULED || old == RUNNING) {
                end()
            }
            SCHEDULED -> begin()
        }
    }

    fun setWorker(worker: Worker<*>?) {
        if (worker !== localWorker) {
            if (localWorker != null) {
                localWorker!!.stateProperty().removeListener(stateListener)
                end()
            }
            localWorker = worker
            if (worker != null) {
                worker.stateProperty().addListener(stateListener)
                if (worker.state == RUNNING || worker.state == SCHEDULED) {
                    // It is already running
                    begin()
                }
            }
        }
    }

    // If the progress indicator changes, then we need to re-initialize
    // If the worker changes, we need to re-initialize
    var dialog: ProgressDialog? = null
    var progressBar: ProgressBar? = null

    constructor(dialog: ProgressDialog) {
        this.dialog = dialog
        this.progressBar = ProgressBar()
        progressBar!!.maxWidth = Double.MAX_VALUE
        getChildren().add(progressBar)
        if (localWorker != null) {
            progressBar?.progressProperty()?.bind(localWorker!!.progressProperty())
        }
    }

    open fun begin() {
        cancelDialogShow = false
        Platform.runLater {
            if (!cancelDialogShow) {
                progressBar!!.progressProperty().bind(localWorker!!.progressProperty())
                dialogVisible = true
                dialog?.show()
            }
        }
    }

    open fun end() {
        progressBar!!.progressProperty().unbind()
        dialogVisible = false
        forcefullyHideDialog(dialog)
    }

    override fun layoutChildren() {
        if (progressBar != null) {
            val insets: Insets = getInsets()
            val w: Double = getWidth() - insets.left - insets.right
            val h: Double = getHeight() - insets.top - insets.bottom
            val prefH = progressBar!!.prefHeight(-1.0)
            val x = insets.left + (w - w) / 2.0
            val y = insets.top + (h - prefH) / 2.0
            progressBar!!.resizeRelocate(x, y, w, prefH)
        }
    }

    open fun forcefullyHideDialog(dialog: Dialog<*>?) {
        if( dialog == null ) return
        val dialogPane = dialog.dialogPane
        if (containsCancelButton(dialog)) {
            dialog.hide()
            return
        }
        dialogPane.buttonTypes.add(ButtonType.CANCEL)
        dialog.hide()
        dialogPane.buttonTypes.remove(ButtonType.CANCEL)
    }

    open fun containsCancelButton(dialog: Dialog<*>): Boolean {
        val dialogPane = dialog.dialogPane
        for (type in dialogPane.buttonTypes) {
            if (type.buttonData == ButtonBar.ButtonData.CANCEL_CLOSE) {
                return true
            }
        }
        return false
    }

}