package com.github.nayasis.kotlin.javafx.stage

import com.github.nayasis.kotlin.basica.core.collection.toList
import com.github.nayasis.kotlin.javafx.scene.*
import javafx.geometry.Rectangle2D
import javafx.scene.Node
import javafx.scene.control.Button
import javafx.stage.Stage
import javafx.stage.Window
import mu.KotlinLogging

private val log = KotlinLogging.logger {}

private const val KEY_BORDERLESS = "KEY_BORDERLESS"

val DEFAULT_ICON = IconContainer()

fun focusedWindow(): Window? {
    for( window in windows())
        if( window.isFocused ) return window
    return null
}

fun windows(): List<Window> {
    return Window.impl_getWindows().toList()
}

fun Stage.loadDefaultIcon(): Stage {
    if( ! DEFAULT_ICON.isEmpty() ) {
        this.icons.addAll(DEFAULT_ICON.icons)
    }
    return this
}

fun Stage.isBorderless(): Boolean {
    return scene?.isBorderless() ?: false
}

fun Stage.setBorderless(option: Stage.() -> Unit = {}, defaultCss: Boolean = true) {
    scene?.setBorderless(defaultCss = defaultCss)
    this.apply(option)
}

fun Stage.addConstraintRetainer() {
    scene?.addConstraintRetainer()
}

fun Stage.addResizeHandler() {
    scene?.addResizeHandler()
}

fun Stage.addMoveHandler(node: Node, buttonClose: Boolean = false, buttonHide: Boolean = false, buttonZoom: Boolean = false, buttonAll: Boolean = false) {
    scene?.addMoveHandler(node = node, buttonClose = buttonClose, buttonHide = buttonHide, buttonZoom = buttonZoom, buttonAll = buttonAll)
}

fun Stage.addClose(button: Button) {
    scene?.addClose(button)
}

fun Stage.addIconified(button: Button) {
    scene?.addIconified(button)
}

fun Stage.addZoomed(button: Button) {
    scene?.addZoomed(button)
}

fun Stage.isZoomed(): Boolean {
    return scene?.isZoomed() ?: false
}

fun Stage.setZoom(enable: Boolean) {
    scene?.setZoom(enable)
}

fun Window.boundary(): Rectangle2D {
    return Rectangle2D(this.x, this.y, this.width, this.height)
}