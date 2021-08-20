package com.github.nayasis.kotlin.javafx.scene

import com.github.nayasis.kotlin.basica.core.extention.FieldProperty
import com.github.nayasis.kotlin.basica.core.string.message
import com.github.nayasis.kotlin.javafx.model.Point
import com.github.nayasis.kotlin.javafx.property.InsetProperty
import com.github.nayasis.kotlin.javafx.stage.BoundaryChecker
import com.github.nayasis.kotlin.javafx.stage.ResizeListener
import javafx.beans.property.ReadOnlyBooleanWrapper
import javafx.event.EventHandler
import javafx.geometry.Insets
import javafx.scene.Node
import javafx.scene.Parent
import javafx.scene.Scene
import javafx.scene.control.Button
import javafx.scene.control.Tooltip
import javafx.scene.input.MouseEvent
import javafx.scene.input.MouseEvent.*
import javafx.scene.layout.HBox
import javafx.scene.layout.Pane
import javafx.scene.layout.Priority
import javafx.scene.layout.Region
import javafx.scene.paint.Color
import javafx.stage.Stage
import javafx.stage.WindowEvent
import tornadofx.add
import tornadofx.addClass
import tornadofx.getChildList
import tornadofx.removeClass
import kotlin.math.abs

private const val KEY_BORDERLESS = "_KEY_BORDERLESS"

fun Scene.isBorderless(): Boolean = properties[KEY_BORDERLESS] == true

fun Scene.setBorderless(defaultCss: Boolean = true) {
    fill = Color.TRANSPARENT // for applying css freely
    if( defaultCss ) {
        root.stylesheets.add("basicafx/css/borderless/root.css")
        window.focusedProperty().addListener { _, _, focused ->
            "root-unfocused".let{
                when {
                    focused -> root.removeClass(it)
                    else    -> root.addClass(it)
                }
            }
        }
    }
    addConstraintRetainer()
    properties[KEY_BORDERLESS] = true
}

fun Scene.addConstraintRetainer() {
    root.let {
        if( it is Pane) {
            widthProperty().addListener  { _, _, new -> it.prefWidth  = new.toDouble() }
            heightProperty().addListener { _, _, new -> it.prefHeight = new.toDouble() }
        }
    }
}

private var Scene.resizeListener: ResizeListener? by FieldProperty{null}

fun Scene.addResizeHandler() {
    if( resizeListener == null )
        resizeListener = ResizeListener(window as Stage)
    listOf(MOUSE_MOVED, MOUSE_PRESSED, MOUSE_DRAGGED, MOUSE_EXITED, MOUSE_EXITED_TARGET)
        .forEach { event -> addEventHandler(event, resizeListener) }
    root.childrenUnmodifiable.forEach{ node -> addResizeListener(node, resizeListener!!) }
}

private fun addResizeListener(node: Node, listener: EventHandler<MouseEvent>) {
    with(node) {
        listOf(MOUSE_MOVED, MOUSE_PRESSED, MOUSE_DRAGGED, MOUSE_EXITED, MOUSE_EXITED_TARGET).forEach { event -> addEventHandler(event, listener) }
        if (this is Parent)
            childrenUnmodifiable.forEach{ addResizeListener(it, listener) }
    }
}

private fun button(type: String): Button {
    return Button().apply {
        tooltip = Tooltip(type.message())
        stylesheets.add("basicafx/css/borderless/button.css")
        isFocusTraversable = false
        addClass("btn-window", "btn-window-$type")

        // set size
        prefWidth = 13.0
        prefHeight = 25.0
        minWidth = prefWidth
        minHeight = prefHeight
    }
}

fun Scene.addMoveHandler(node: Node, buttonClose: Boolean = false, buttonHide: Boolean = false, buttonZoom: Boolean = false, buttonAll: Boolean = false) {

    val drawButton = buttonAll || buttonClose || buttonHide || buttonZoom

    val stage = this
    val handler = if(drawButton) {
        val children = node.parent.getChildList()
        if( children != null ) {
            val idx = children.indexOf(node)
            val hbox = HBox().apply {
                styleClass.add("menu-bar")
                add(node)
                add(Region().apply { HBox.setHgrow(this, Priority.ALWAYS) })
                add(HBox().apply {
                    if( buttonAll || buttonClose ) add(button("close").also { stage.addClose(it) })
                    if( buttonAll || buttonHide  ) add(button("hide").also { stage.addIconified(it) })
                    if( buttonAll || buttonZoom  ) add(button("zoom").also { stage.addZoomed(it) })
                    spacing = 3.0
                    padding = Insets(0.0, 5.0, 0.0, 0.0)
                })
            }
            children.remove(hbox)
            children.add(idx, hbox)
            hbox
        } else node
    } else node

    val offset = Point()

    with(handler) {
        setOnMouseClicked { e ->
            if( e.clickCount <= 1 || resizeListener == null ) return@setOnMouseClicked
            setZoom( ! isZoomed() )
        }
        setOnMousePressed { e ->
            offset.x = e.sceneX
            offset.y = e.sceneY
        }
        setOnMouseDragged { e ->
            if( isZoomed() ) {
                setZoom(false)
                val half = width / 2
                val screen = BoundaryChecker.getScreenContains(x, y)!!.visualBounds
                when {
                    // out over left
                    ! screen.contains(e.screenX - half, y) -> {
                        window.x = screen.minX
                        offset.x = abs(e.screenX - screen.minX)
                    }
                    // out over right
                    ! screen.contains(e.screenX + half, y) -> {
                        val margin = half + abs((e.screenX + half) - screen.maxX)
                        window.x = e.screenX - margin
                        offset.x = margin
                    }
                    else -> {
                        window.x = e.screenX - half
                        offset.x = + half
                    }
                }
            } else {
                window.x = e.screenX - offset.x
                window.y = e.screenY - offset.y
            }
        }
    }

}

fun Scene.addClose(button: Button) {
    button.setOnAction { window.fireEvent(WindowEvent(this.window, WindowEvent.WINDOW_CLOSE_REQUEST)) }
}

fun Scene.addIconified(button: Button) {
    button.setOnAction { (window as Stage).isIconified = true }
}

fun Scene.addZoomed(button: Button) {
    button.setOnAction {
        setZoom(!isZoomed())
    }
}

var Scene.zoomed: ReadOnlyBooleanWrapper by FieldProperty{ ReadOnlyBooleanWrapper(it, "zoomed") }

fun Scene.isZoomed(): Boolean {
    return zoomed.get()
}

var Scene.previousZoomSize: InsetProperty? by FieldProperty{ null }

fun Scene.setZoom(enable: Boolean) {
    zoomed.set(enable)
    if( enable ) {
        previousZoomSize = InsetProperty(window)
        BoundaryChecker.getMajorScreen(window).visualBounds.let {
            window.x      = it.minX
            window.y      = it.minY
            window.width  = it.width
            window.height = it.height
        }
    } else {
        previousZoomSize?.bind(window)
        previousZoomSize = null
    }

}