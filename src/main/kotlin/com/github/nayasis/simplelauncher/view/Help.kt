package com.github.nayasis.simplelauncher.view

import com.github.nayasis.kotlin.basica.core.string.message
import javafx.scene.web.WebView
import mu.KotlinLogging
import tornadofx.View
import tornadofx.runLater
import tornadofx.vbox
import tornadofx.webview

private val logger = KotlinLogging.logger {}

class Help: View("stage.help".message()) {
    lateinit var webview: WebView
    override val root = vbox {
        val self = this
        webview = webview {
            prefHeightProperty().bind(self.heightProperty())
            prefWidthProperty().bind(self.widthProperty())
            runLater {
                logger.debug { ">> help page : ${"stage.help.url".message()}" }
                engine.load("stage.help.url".message())
            }
        }
    }
}