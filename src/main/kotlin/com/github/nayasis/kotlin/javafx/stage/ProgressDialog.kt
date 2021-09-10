package com.github.nayasis.kotlin.javafx.stage

import javafx.beans.value.ChangeListener
import javafx.concurrent.Worker
import javafx.concurrent.Worker.State.*
import javafx.scene.Scene
import javafx.scene.control.ButtonType.CANCEL
import javafx.scene.control.Dialog
import javafx.scene.control.Label
import javafx.scene.control.ProgressBar
import javafx.scene.layout.Region
import javafx.scene.layout.VBox
import javafx.scene.paint.Color
import javafx.stage.Stage
import javafx.stage.StageStyle
import javafx.util.Callback
import mu.KotlinLogging
import tornadofx.runLater

private val logger = KotlinLogging.logger {}

class ProgressDialog<T>: Dialog<T> {

    val scene: Scene = dialogPane.scene.apply {
        fill = Color.TRANSPARENT
        stylesheets.add("basicafx/css/dialog-progress.css")
    }
    val stage: Stage = (scene.window as Stage).apply {
        initStyle(StageStyle.TRANSPARENT)
        isAlwaysOnTop = true
        loadDefaultIcon()
        addMoveHandler(dialogPane)
    }

    private var progressPane: ProgressPane<T>
    private val message = Label()

    constructor(worker: Worker<T>?) {

        if( worker?.state in setOf(CANCELLED, FAILED, SUCCEEDED) )
            throw IllegalArgumentException("worker state is not valid (${worker?.state})")

        resultConverter = Callback { null }

        // set view
        progressPane = ProgressPane(this,worker)
        dialogPane.content = VBox(10.0, message, progressPane).apply {
            maxWidth = Double.MAX_VALUE
            setPrefSize(400.0, 40.0)
        }

        worker?.titleProperty()?.addListener { _, _, text -> dialogPane.headerText = text ?: "" }
        worker?.messageProperty()?.addListener { _, _, text -> message.text = text ?: "" }

    }

    fun updateProgress(workDone: Long, max: Long) {
        updateProgress(workDone.toDouble(),max.toDouble())
    }

    fun updateProgress(workDone: Double, max: Double) {
        runLater {
            progressPane.progressBar.progress = when {
                workDone.isInfinite() || workDone.isNaN() -> 0.0
                max.isInfinite() || max.isNaN() -> 0.0
                else -> workDone / max
            }
        }
    }

    fun updateTitle(text: String?) {
        text?.let { runLater { dialogPane.headerText = it }}
    }

    fun updateMessage(message: String?) {
        message?.let { runLater { this.message.text = it }}
    }

    fun closeForcibly() {
        runLater { progressPane.end() }
    }

}

private class ProgressPane<T>(
    val dialog: ProgressDialog<T>,
    val worker: Worker<T>?,
): Region() {

    private val stateListener = ChangeListener<Worker.State> { _, _, state ->
        when (state) {
            CANCELLED, FAILED, SUCCEEDED -> end()
            SCHEDULED -> begin()
        }
    }

    val progressBar: ProgressBar = ProgressBar().apply {
        maxWidth = Double.MAX_VALUE
        worker?.let { progressProperty().bind(it.progressProperty()) }
    }

    init {
        children.add(progressBar)
        worker?.stateProperty()?.addListener(stateListener)
    }

    fun begin() {
        runLater {
            dialog.show()
        }
    }

    fun end() {
        progressBar.progressProperty().unbind()
        hideForcibly(dialog)
        dialog.close()
    }

    override fun layoutChildren() {
        val insets = insets
        val w = width - insets.left - insets.right
        val h = height - insets.top - insets.bottom
        val prefH = progressBar.prefHeight(-1.0)
        val x = insets.left + (w - w) / 2.0
        val y = insets.top + (h - prefH) / 2.0
        progressBar.resizeRelocate(x, y, w, prefH)
    }

    private fun hideForcibly(dialog: Dialog<*>) {
        with(dialog) {
            dialogPane.buttonTypes.add(CANCEL)
            hide()
            dialogPane.buttonTypes.remove(CANCEL)
        }
    }

}