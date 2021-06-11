package com.github.nayasis.simplelauncher.view

import com.github.nayasis.kotlin.basica.core.validator.nvl
import com.github.nayasis.kotlin.javafx.preloader.NPreloader
import javafx.geometry.Pos
import javafx.scene.Scene
import javafx.scene.control.Label
import javafx.scene.control.ProgressBar
import javafx.scene.layout.AnchorPane
import javafx.scene.layout.VBox
import javafx.stage.Stage
import javafx.stage.StageStyle

class Preloader: NPreloader() {

    override fun start(primaryStage: Stage?) {

        val root  = AnchorPane().apply { id = "splash" }
        val scene = Scene(root, 527.0, 297.0).apply { stylesheets.add("/view/splash.css") }
        val stage = Stage(StageStyle.TRANSPARENT).apply {
            setScene(scene)
            isAlwaysOnTop = true
        }

        root.children.addAll(statusLayout())
        super.stage = stage

        stage.show()

    }

    fun statusLayout(): VBox {
        val label = Label()
        val progressbar = ProgressBar().apply { maxWidth = Double.MAX_VALUE }

        handler = { message, percentage ->
            label.text = nvl(message)
            progressbar.progress = (percentage?:0.0)/100
        }

        return VBox().apply {
            alignment = Pos.CENTER_RIGHT
            AnchorPane.setLeftAnchor(this, 0.0)
            AnchorPane.setRightAnchor(this, 0.0)
            AnchorPane.setBottomAnchor(this, 0.0)
            children.addAll(progressbar, label)
        }
    }
}