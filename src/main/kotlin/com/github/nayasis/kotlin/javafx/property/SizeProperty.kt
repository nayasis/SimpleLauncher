package com.github.nayasis.kotlin.javafx.property

import javafx.stage.Stage
import java.io.Serializable

data class SizeProperty(
    var width: Int = 400,
    var height: Int = 300,
): Serializable {
    fun read(stage: Stage) {
        width = stage.width.toInt()
        height = stage.height.toInt()
    }
    fun bind(stage: Stage) {
        stage.width = width.toDouble()
        stage.height = height.toDouble()
    }
}