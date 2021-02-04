package com.github.nayasis.kotlin.javafx.stage

import com.github.nayasis.kotlin.basica.FieldProperty
import com.github.nayasis.kotlin.basica.message
import com.github.nayasis.kotlin.basica.toList
import com.github.nayasis.kotlin.javafx.model.Point
import com.github.nayasis.kotlin.javafx.property.InsetProperty
import javafx.beans.property.ReadOnlyBooleanWrapper
import javafx.event.EventHandler
import javafx.geometry.Insets
import javafx.geometry.Point2D
import javafx.geometry.Rectangle2D
import javafx.scene.Node
import javafx.scene.Parent
import javafx.scene.control.Button
import javafx.scene.control.Tooltip
import javafx.scene.image.Image
import javafx.scene.input.MouseEvent
import javafx.scene.input.MouseEvent.*
import javafx.scene.layout.HBox
import javafx.scene.layout.Pane
import javafx.scene.layout.Priority
import javafx.scene.layout.Region
import javafx.stage.Stage
import javafx.stage.Window
import javafx.stage.WindowEvent
import mu.KotlinLogging
import tornadofx.add
import tornadofx.addClass
import tornadofx.getChildList
import java.util.*
import kotlin.collections.ArrayList
import kotlin.math.abs

private val log = KotlinLogging.logger {}

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
//    initStyle(StageStyle.TRANSPARENT)
//    scene?.fill = Color.TRANSPARENT
    addConstraintRetainer()
    addResizeHandler()
    this.apply(option)
}

fun Stage.addConstraintRetainer() {
    scene?.root.let {
        if( it is Pane ) {
            scene.widthProperty().addListener  { _,_,new -> it.prefWidth  = new.toDouble() }
            scene.heightProperty().addListener { _,_,new -> it.prefHeight = new.toDouble() }
        }
    }
}

private var Stage.resizeListener: ResizeListener? by FieldProperty{null}

fun Stage.addResizeHandler() {
    if( resizeListener == null )
        resizeListener = ResizeListener(this)
    scene?.let{
        listOf(MOUSE_MOVED,MOUSE_PRESSED,MOUSE_DRAGGED,MOUSE_EXITED,MOUSE_EXITED_TARGET).forEach { event -> addEventHandler(event, resizeListener) }
        it.root.childrenUnmodifiable.forEach{ node -> addResizeListener(node,resizeListener!!) }
    }
}

private fun addResizeListener(node: Node, listener: EventHandler<MouseEvent>) {
    with(node) {
        listOf(MOUSE_MOVED,MOUSE_PRESSED,MOUSE_DRAGGED,MOUSE_EXITED,MOUSE_EXITED_TARGET).forEach { event -> addEventHandler(event, listener) }
        if (this is Parent)
            childrenUnmodifiable.forEach{ addResizeListener(it,listener) }
    }
}

private fun button(type: String): Button {
    return Button().apply {
        tooltip = Tooltip(type.message())
        stylesheets.add("basicafx/css/button.css")
        isFocusTraversable = false
        addClass("btn-window","btn-window-$type")
    }
}
private fun buttonsWindow(stage: Stage): HBox {
    return HBox().apply {
        add(button("hide").also{stage.addIconified(it)})
        add(button("zoom").also{stage.addZoomed(it)})
        add(button("close").also{stage.addClose(it)})
        spacing = 2.0
        padding = Insets(0.0,5.0,0.0,0.0)
    }
}

fun Stage.addMoveHandler(node: Node, buttons: Boolean = false) {

    val stage = this
    var handler = if(buttons) {
        val children = node.parent.getChildList()
        if( children != null ) {
            var idx = children.indexOf(node)
            val hbox = HBox().apply {
                add(node)
                add(Region().apply {HBox.setHgrow(this,Priority.ALWAYS)})
                add(buttonsWindow(stage))
            }
            children.remove(hbox)
            children.add(idx,hbox)
            hbox
        } else node
    } else node

    val offset = Point()

    with(handler) {
        setOnMouseClicked { e ->
            if( e.clickCount <= 1 ) return@setOnMouseClicked
            setZoom(true)
        }
        setOnMousePressed { e ->
            offset.x = e.sceneX
            offset.y = e.sceneY
        }
        setOnMouseDragged { e ->
            if(resizeListener == null || resizeListener!!.onDragged()) return@setOnMouseDragged
            if( isZoomed() ) {
                setZoom(false)
                val half = width / 2
                val screen = BoundaryChecker.getScreenContains(x,y)!!.visualBounds
                when {
                    // out over left
                    ! screen.contains(e.screenX - half, y) -> {
                        x = screen.minX
                        offset.x = abs(e.screenX - screen.minX)
                    }
                    // out over right
                    ! screen.contains(e.screenX + half, y) -> {
                        val margin = half + abs((e.screenX + half) - screen.maxX)
                        x = e.screenX - margin
                        offset.x = margin
                    }
                    else -> {
                        x = e.screenX - half
                        offset.x = + half
                    }
                }
            } else {
                x = e.screenX - offset.x
                y = e.screenY - offset.y
            }
        }
    }

}

fun Stage.addClose(button: Button) {
    button.setOnAction { fireEvent(WindowEvent(this, WindowEvent.WINDOW_CLOSE_REQUEST)) }
}

fun Stage.addIconified(button: Button) {
    button.setOnAction { this.isIconified = true }
}

fun Stage.addZoomed(button: Button) {
    button.setOnAction {
        setZoom( ! isZoomed() )
    }
}

var Stage.zoomed: ReadOnlyBooleanWrapper by FieldProperty{ ReadOnlyBooleanWrapper(it,"zoomed") }

fun Stage.isZoomed(): Boolean {
    return zoomed.get()
}

var Stage.previousZoomSize: InsetProperty? by FieldProperty{ null }

fun Stage.setZoom( enable: Boolean ) {
    zoomed.set(enable)
    if( enable ) {
        previousZoomSize = InsetProperty(this)
        BoundaryChecker.getMajorScreen(this).visualBounds.let {
            this.x      = it.minX
            this.y      = it.minY
            this.width  = it.width
            this.height = it.height
        }
    } else {
        previousZoomSize?.apply(this)
        previousZoomSize = null
    }

}

fun Stage.boundary(): Rectangle2D {
    return Rectangle2D(this.x, this.y, this.width, this.height)
}

fun Stage.startPoint(): Point2D {
    return Point2D(x,y)
}