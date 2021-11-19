package com.github.nayasis.simplelauncher.view

import com.github.nayasis.kotlin.javafx.control.basic.bottomAnchor
import com.github.nayasis.kotlin.javafx.control.basic.leftAnchor
import com.github.nayasis.kotlin.javafx.control.basic.rightAnchor
import com.github.nayasis.kotlin.javafx.preloader.CloseNotificator
import com.github.nayasis.kotlin.javafx.preloader.NPreloader
import com.github.nayasis.kotlin.javafx.preloader.ProgressNotificator
import com.github.nayasis.kotlin.javafx.stage.addMoveHandler
import com.github.nayasis.kotlin.javafx.stage.loadDefaultIcon
import com.github.nayasis.kotlin.tornadofx.extension.toScene
import javafx.geometry.Pos
import javafx.scene.control.Label
import javafx.scene.control.ProgressBar
import javafx.stage.Stage
import javafx.stage.StageStyle
import tornadofx.*

class Splash: NPreloader() {

    private val view = PreloaderLayout()
    private lateinit var stage: Stage

    override fun start(primaryStage: Stage) {
        stage = primaryStage.apply {
            initStyle(StageStyle.TRANSPARENT)
            scene = view.toScene("/view/splash/splash.css")
            isAlwaysOnTop = true
            loadDefaultIcon()
            addMoveHandler(view.root)
            show()
        }
    }

    override fun onProgress(notificator: ProgressNotificator) {
        with(notificator) {
            message?.let { view.label.text = it }
            view.progressBar.progress = progress
        }
    }

    override fun onClose(notificator: CloseNotificator) {
        runLater { stage.close() }
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
                progress = 0.0
            }
            label = label { ellipsisString = "..." }
            leftAnchor = 0.0
            rightAnchor = 0.0
            bottomAnchor = 0.0
        }
    }

}