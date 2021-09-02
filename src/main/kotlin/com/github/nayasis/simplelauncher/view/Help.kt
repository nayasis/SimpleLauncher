package com.github.nayasis.simplelauncher.view

import com.github.nayasis.kotlin.basica.core.string.message
import tornadofx.View
import tornadofx.vbox
import tornadofx.webview

class Help: View("stage.help".message()) {
    override val root = vbox {
        val self = this
        webview {
            prefHeightProperty().bind(self.heightProperty())
            prefWidthProperty().bind(self.widthProperty())
            engine.load("stage.help.url".message())
        }
    }
}