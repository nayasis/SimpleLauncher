package com.github.nayasis.kotlin.javafx.stage

import com.github.nayasis.kotlin.javafx.scene.*
import javafx.event.EventHandler
import javafx.geometry.Rectangle2D
import javafx.scene.Node
import javafx.scene.Scene
import javafx.scene.control.Button
import javafx.stage.Stage
import javafx.stage.Window
import javafx.stage.WindowEvent
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

class Stages { companion object {

    val defaultIcons = IconContainer()

    val focusedWindow: Window?
        get() {
            for( window in windows)
                if( window.isFocused ) return window
            return null
        }

    val windows: List<Window>
        get() = Window.getWindows()

}}

fun Stage.loadDefaultIcon(): Stage {
    if( ! Stages.defaultIcons.isEmpty() ) {
        this.icons.addAll(Stages.defaultIcons.icons)
    }
    return this
}

val Stage.isBorderless: Boolean
    get() = scene?.isBorderless() ?: false

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

val Stage.isZoomed: Boolean
    get() = scene?.isZoomed() ?: false

fun Stage.setZoom(enable: Boolean) {
    scene?.setZoom(enable)
}

fun Stage.addCloseRequest(event: EventHandler<WindowEvent>) =
    this.addEventFilter(WindowEvent.WINDOW_CLOSE_REQUEST,event)

val Stage.focusedNode: Node?
    get() = this.scene?.focusOwnerProperty()?.get()

val Window.boundary: Rectangle2D
    get() = Rectangle2D(this.x, this.y, this.width, this.height)

val Scene?.stage: Stage?
    get() = this?.window as Stage