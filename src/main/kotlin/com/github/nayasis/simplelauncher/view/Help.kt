package com.github.nayasis.simplelauncher.view

import com.github.nayasis.kotlin.basica.core.string.message
import com.github.nayasis.kotlin.basica.net.Networks
import com.github.nayasis.kotlin.javafx.property.SizeProperty
import com.github.nayasis.simplelauncher.service.ConfigService
import javafx.scene.web.WebView
import mu.KotlinLogging
import tornadofx.View
import tornadofx.runLater
import tornadofx.vbox
import tornadofx.webview

private val logger = KotlinLogging.logger {}

class Help: View("stage.help".message()) {

    init {
        // accept https url
        Networks.trustAllCerts()
    }

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

    override fun onBeforeShow() {
        ConfigService.stageHelp?.bind(currentStage!!)
    }

    override fun onUndock() {
        with(ConfigService) {
            stageHelp = SizeProperty(currentStage)
            save()
        }
    }

}