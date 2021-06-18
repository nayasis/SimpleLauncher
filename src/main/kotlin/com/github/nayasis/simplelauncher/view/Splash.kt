package com.github.nayasis.simplelauncher.view

import com.github.nayasis.kotlin.javafx.preloader.CloseNotificator
import com.github.nayasis.kotlin.javafx.preloader.NPreloader
import com.github.nayasis.kotlin.javafx.preloader.ProgressNotificator
import com.github.nayasis.kotlin.tornadofx.extension.toScene
import javafx.application.Platform
import javafx.geometry.Pos
import javafx.scene.control.Label
import javafx.scene.control.ProgressBar
import javafx.scene.layout.AnchorPane
import javafx.stage.Stage
import javafx.stage.StageStyle
import tornadofx.View
import tornadofx.anchorpane
import tornadofx.label
import tornadofx.progressbar
import tornadofx.vbox

class Splash: NPreloader() {

    private val view = PreloaderLayout()
    private lateinit var stage: Stage

    override fun start(primaryStage: Stage) {
        stage = primaryStage.apply {
            initStyle(StageStyle.TRANSPARENT)
            scene = view.toScene("/view/splash/splash.css")
            isAlwaysOnTop = true
            show()
        }
    }

    override fun onProgress(notificator: ProgressNotificator) {
        with(notificator) {
            message?.let { view.label.text = it }
            view.progressBar.progress = progress * 100
        }
    }

    override fun onClose(notificator: CloseNotificator) {
        Platform.runLater { stage.close() }
    }

}

class PreloaderLayout: View() {

    lateinit var progressBar: ProgressBar
    lateinit var label: Label

    override val root = anchorpane {
        id = "splash"
        prefWidth = 527.0
        prefHeight = 297.0
        vbox {
            alignment = Pos.CENTER_RIGHT
            progressBar = progressbar {
                maxWidth = Double.MAX_VALUE
            }
            label = label {  }
            AnchorPane.setLeftAnchor(this, 0.0)
            AnchorPane.setRightAnchor(this, 0.0)
            AnchorPane.setBottomAnchor(this, 0.0)
        }
    }

}