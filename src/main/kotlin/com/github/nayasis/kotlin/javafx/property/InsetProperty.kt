package com.github.nayasis.kotlin.javafx.property

import javafx.stage.Window
import java.io.Serializable

data class InsetProperty(
    var x:      Int = 0,
    var y:      Int = 0,
    var width:  Int = 400,
    var height: Int = 300,
): Serializable {
    constructor(window: Window): this(
        x = window.x.toInt(),
        y = window.y.toInt(),
        width = window.width.toInt(),
        height = window.height.toInt(),
    )
    fun read(window: Window) {
        x = window.x.toInt()
        y = window.y.toInt()
        width = window.width.toInt()
        height = window.height.toInt()
    }
    fun bind(window: Window) {
        window.x = x.toDouble()
        window.y = y.toDouble()
        window.width = width.toDouble()
        window.height = height.toDouble()
    }
}