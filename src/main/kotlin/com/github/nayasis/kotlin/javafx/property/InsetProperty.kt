package com.github.nayasis.kotlin.javafx.property

import javafx.stage.Stage
import java.io.Serializable

data class InsetProperty(
    var x:      Int = 0,
    var y:      Int = 0,
    var width:  Int = 400,
    var height: Int = 300,
): Serializable {
    constructor(stage: Stage): this(
        x = stage.x.toInt(),
        y = stage.y.toInt(),
        width = stage.width.toInt(),
        height = stage.height.toInt(),
    )
    fun read(stage: Stage) {
        x = stage.x.toInt()
        y = stage.y.toInt()
        width = stage.width.toInt()
        height = stage.height.toInt()
    }
    fun apply(stage: Stage) {
        stage.x = x.toDouble()
        stage.y = y.toDouble()
        stage.width = width.toDouble()
        stage.height = height.toDouble()
    }
}