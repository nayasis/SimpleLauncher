package com.github.nayasis.simplelauncher.view

import com.github.nayasis.kotlin.basica.core.string.message
import com.github.nayasis.kotlin.javafx.property.SizeProperty
import com.github.nayasis.kotlin.javafx.stage.Dialog
import com.github.nayasis.simplelauncher.common.Context
import javafx.scene.web.WebView
import tornadofx.View
import tornadofx.runLater
import tornadofx.vbox
import tornadofx.webview

class Help: View("stage.help".message()) {

    private lateinit var webview: WebView

    override val root = vbox {
        val self = this
        webview = webview {
            prefHeightProperty().bind(self.heightProperty())
            prefWidthProperty().bind(self.widthProperty())
            runLater {
                try {
                    engine.load("stage.help.url".message())
                } catch (e: Exception) {
                    Dialog.error(e)
                }
            }
        }
    }

    override fun onBeforeShow() {
        Context.config.stageHelp?.bind(currentStage!!)
    }

    override fun onUndock() {
        Context.config.run {
            stageHelp = SizeProperty(currentStage)
        }
    }

}