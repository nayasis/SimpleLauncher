package com.github.nayasis.simplelauncher.view.terminal

import com.github.nayasis.kotlin.basica.core.string.toResource
import com.github.nayasis.kotlin.basica.exec.Command
import javafx.geometry.Pos
import javafx.scene.Scene
import javafx.scene.layout.VBox
import javafx.scene.web.WebView
import javafx.stage.Stage
import tornadofx.App
import tornadofx.launch
import kotlin.system.exitProcess

fun main(args: Array<String>) {
    launch<LocalWebViewTest>(args)
}

class LocalWebViewTest: App() {
    override fun start(stage: Stage) {

        val webview = WebView()
        webview.engine.load("view/test.html".toResource()!!.toExternalForm())

        val root = VBox(webview).apply {
            alignment = Pos.CENTER
        }

        stage.scene = Scene(root, 640.0, 480.0)
        stage.show()

    }
}