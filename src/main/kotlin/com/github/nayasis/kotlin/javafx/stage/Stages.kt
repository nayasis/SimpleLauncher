package com.github.nayasis.kotlin.javafx.stage

import com.github.nayasis.kotlin.basica.FieldProperty
import com.github.nayasis.kotlin.basica.toList
import com.github.nayasis.kotlin.javafx.model.Point
import com.github.nayasis.kotlin.javafx.property.InsetProperty
import javafx.beans.property.ReadOnlyBooleanProperty
import javafx.beans.property.ReadOnlyBooleanWrapper
import javafx.event.EventHandler
import javafx.geometry.Rectangle2D
import javafx.scene.Node
import javafx.scene.Parent
import javafx.scene.control.Button
import javafx.scene.image.Image
import javafx.scene.input.MouseEvent
import javafx.scene.layout.Pane
import javafx.scene.paint.Color
import javafx.stage.Screen
import javafx.stage.Stage
import javafx.stage.StageStyle
import javafx.stage.Window
import javafx.stage.WindowEvent
import java.util.*
import kotlin.collections.ArrayList
import kotlin.reflect.KProperty

val DEFAULT_ICONS = ArrayList<Image>()

fun focusedWindow(): Window? {
    for( window in windows())
        if( window.isFocused ) return window
    return null
}

fun windows(): List<Window> {
    return Window.impl_getWindows().toList()
}

fun Stage.loadDefaultIcon() {
    if( DEFAULT_ICONS.isNotEmpty() ) {
        this.icons.addAll(DEFAULT_ICONS)
    }
}

fun Stage.setBorderless(option:Stage.() -> Unit = {}) {
    initStyle(StageStyle.TRANSPARENT)
    scene.fill = Color.TRANSPARENT
    addConstraintRetainer()
    addResizeHandler()
    this.apply(option)
}

fun Stage.addConstraintRetainer() {
    scene.root.let {
        if( it is Pane ) {
            scene.widthProperty().addListener  { _,_,new -> it.prefWidth  = new.toDouble() }
            scene.heightProperty().addListener { _,_,new -> it.prefHeight = new.toDouble() }
        }
    }
}

fun Stage.addResizeHandler() {
    val listener = ResizeListener(this)
    with(this.scene) {
        addEventHandler(MouseEvent.MOUSE_MOVED, listener)
        addEventHandler(MouseEvent.MOUSE_PRESSED, listener)
        addEventHandler(MouseEvent.MOUSE_DRAGGED, listener)
        addEventHandler(MouseEvent.MOUSE_EXITED, listener)
        addEventHandler(MouseEvent.MOUSE_EXITED_TARGET, listener)
        root.childrenUnmodifiable.forEach{ addResizeListener(it,listener) }
    }
}

private fun addResizeListener(node: Node, listener: EventHandler<MouseEvent>) {
    with(node) {
        addEventHandler(MouseEvent.MOUSE_MOVED, listener)
        addEventHandler(MouseEvent.MOUSE_PRESSED, listener)
        addEventHandler(MouseEvent.MOUSE_DRAGGED, listener)
        addEventHandler(MouseEvent.MOUSE_EXITED, listener)
        addEventHandler(MouseEvent.MOUSE_EXITED_TARGET, listener)
        if (this is Parent)
            childrenUnmodifiable.forEach{ addResizeListener(it,listener) }
    }
}

fun Stage.addMoveHandler( handler: Node ) {

    val offset = Point()

    with(handler) {
        setOnMouseClicked { e ->
            if( e.clickCount <= 1 ) return@setOnMouseClicked
            isMaximized = true
        }
        setOnMousePressed { e ->
            offset.x = e.sceneX
            offset.y = e.sceneY
        }
        setOnMouseDragged { e ->
            if( isMaximized )
                isMaximized = false
            x = e.screenX - offset.x
            y = e.screenY - offset.y
        }
    }

}

fun Stage.addClose(button: Button) {
    button.setOnAction { fireEvent(WindowEvent(this, WindowEvent.WINDOW_CLOSE_REQUEST)) }
}

fun Stage.addIconified(button: Button) {
    button.setOnAction { this.isIconified = true }
}

fun Stage.addMaximized(button: Button) {
    button.setOnAction {
        isMaximized = ! isMaximized
    }
}

var Stage.isZoomed: ReadOnlyBooleanWrapper by FieldProperty{ ReadOnlyBooleanWrapper(it,"zoomed") }

var Stage.previousZoomSize: InsetProperty? by FieldProperty{ null }

fun Stage.setZoom( enable: Boolean ) {
    isZoomed.set(enable)
    if( enable ) {
        if( maximizedProperty() != null ) {
            previousZoomSize = InsetProperty(this)
            Screen.getScreensForRectangle(Rectangle2D(x,y,width,height)).get(0)?.visualBounds?.let {
                this.x      = it.minX
                this.y      = it.minY
                this.width  = it.width
                this.height = it.height
            }
        }
    } else {
        previousZoomSize?.apply(this)
        previousZoomSize = null
    }

}