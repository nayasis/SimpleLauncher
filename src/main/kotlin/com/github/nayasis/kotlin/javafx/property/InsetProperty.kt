package com.github.nayasis.kotlin.javafx.property

import com.github.nayasis.kotlin.javafx.stage.BoundaryChecker
import javafx.scene.control.Dialog
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
    constructor(dialog: Dialog<*>): this(
        x = dialog.x.toInt(),
        y = dialog.y.toInt(),
        width = dialog.width.toInt(),
        height = dialog.height.toInt(),
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
        BoundaryChecker.reset(window)
    }
    fun bind(dialog: Dialog<*>) {
        dialog.x = x.toDouble()
        dialog.y = y.toDouble()
        dialog.width = width.toDouble()
        dialog.height = height.toDouble()
        BoundaryChecker.reset(dialog)
    }
}