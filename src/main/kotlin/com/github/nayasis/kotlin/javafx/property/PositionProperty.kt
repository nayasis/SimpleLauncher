package com.github.nayasis.kotlin.javafx.property

import javafx.stage.Stage
import java.io.Serializable

data class PositionProperty(
    var x: Int = 0,
    var y: Int = 0,
): Serializable {
    fun read(stage: Stage) {
        x = stage.x.toInt()
        y = stage.y.toInt()
    }
    fun bind(stage: Stage) {
        stage.x = x.toDouble()
        stage.y = y.toDouble()
    }
}