package com.github.nayasis.kotlin.tornadofx.extension

import javafx.scene.Scene
import javafx.stage.Stage
import tornadofx.View

fun View.toScene(cssResourcePath: String = ""): Scene {
    return Scene(root).apply {
        if(cssResourcePath.isNotEmpty())
            stylesheets.add(cssResourcePath)
    }
}

fun View.toStage(cssResourcePath: String = ""): Stage {
    val scene = toScene(cssResourcePath)
    return Stage().apply { this.scene = scene }
}