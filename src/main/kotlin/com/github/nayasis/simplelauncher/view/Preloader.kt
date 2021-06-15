package com.github.nayasis.simplelauncher.view

import com.github.nayasis.kotlin.javafx.preloader.CloseNotificator
import com.github.nayasis.kotlin.javafx.preloader.NPreloader
import com.github.nayasis.kotlin.javafx.preloader.ProgressNotificator
import javafx.application.Platform
import javafx.geometry.Pos
import javafx.scene.Scene
import javafx.scene.control.Label
import javafx.scene.control.ProgressBar
import javafx.scene.layout.AnchorPane
import javafx.scene.layout.VBox
import javafx.stage.Stage
import javafx.stage.StageStyle

class Preloader: NPreloader() {

    private val label = Label()
    private val progressbar = ProgressBar().apply { maxWidth = Double.MAX_VALUE }
    private lateinit var stage: Stage

    override fun start(primaryStage: Stage?) {

        val root  = AnchorPane().apply { id = "splash" }
        val scene = Scene(root, 527.0, 297.0).apply { stylesheets.add("/view/splash.css") }
        stage = Stage(StageStyle.TRANSPARENT).apply {
            setScene(scene)
            isAlwaysOnTop = true
        }

        val layout = VBox().apply {
            alignment = Pos.CENTER_RIGHT
            AnchorPane.setLeftAnchor(this, 0.0)
            AnchorPane.setRightAnchor(this, 0.0)
            AnchorPane.setBottomAnchor(this, 0.0)
            children.addAll(progressbar, label)
        }

        root.children.addAll(layout)

        stage.show()

    }

    override fun onProgress(notificator: ProgressNotificator) {
        with(notificator) {
            message?.let { label.text = it }
            progressbar.progress = progress * 100
        }
    }

    override fun onClose(notificator: CloseNotificator) {
        stage.close()
        Platform.runLater { stage.close() }
    }

}