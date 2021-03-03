package com.github.nayasis.kotlin.javafx.stage

import com.github.nayasis.kotlin.javafx.model.Point
import javafx.event.EventHandler
import javafx.scene.Cursor
import javafx.scene.input.MouseEvent
import javafx.scene.input.MouseEvent.*
import javafx.stage.Stage
import mu.KotlinLogging

private val log = KotlinLogging.logger{}

class ResizeListener(
    private val stage: Stage?,
    private val margin: Int = 5
): EventHandler<MouseEvent> {

    private val position: Point = Point()
    private var cursor: Cursor = Cursor.DEFAULT

    fun onDragged(): Boolean {
        return cursor != Cursor.DEFAULT
    }

    override fun handle(event: MouseEvent) {

        if( stage == null || stage.scene == null ) return

        val eventType = event.eventType
        val scene     = stage.scene
        val pointer   = Point(event)

        when {
            MOUSE_MOVED == eventType -> {
                cursor = when {
                    pointer.x <= margin && pointer.y <= margin                              -> Cursor.NW_RESIZE
                    pointer.x <= margin && pointer.y >= scene.height - margin               -> Cursor.SW_RESIZE
                    pointer.x >= scene.width - margin && pointer.y <= margin                -> Cursor.NE_RESIZE
                    pointer.x >= scene.width - margin && pointer.y >= scene.height - margin -> Cursor.SE_RESIZE
                    pointer.x <= margin                                                     -> Cursor.W_RESIZE
                    pointer.x >= scene.width - margin                                       -> Cursor.E_RESIZE
                    pointer.y <= margin                                                     -> Cursor.N_RESIZE
                    pointer.y >= scene.height - margin                                      -> Cursor.S_RESIZE
                    else                                                                    -> Cursor.DEFAULT
                }
                scene.cursor = cursor
            }
            eventType in listOf(MOUSE_EXITED,MOUSE_EXITED_TARGET) -> {
                scene.cursor = Cursor.DEFAULT
            }
            eventType == MOUSE_PRESSED -> {
                position.x = stage.width  - pointer.x
                position.y = stage.height - pointer.y
            }
            eventType == MOUSE_DRAGGED -> {
                if ( cursor == Cursor.DEFAULT ) return
                if ( cursor !in listOf(Cursor.W_RESIZE,Cursor.E_RESIZE) ) {
                    val minHeight = if (stage.minHeight > margin * 2) stage.minHeight else (margin * 2).toDouble()
                    if ( cursor in listOf(Cursor.NW_RESIZE,Cursor.N_RESIZE,Cursor.NE_RESIZE) ) {
                        if (stage.height > minHeight || pointer.y < 0) {
                            stage.height = stage.y - event.screenY + stage.height
                            stage.y = event.screenY
                        }
                    } else {
                        if (stage.height > minHeight || pointer.y + position.y - stage.height > 0) {
                            stage.height = pointer.y + position.y
                        }
                    }
                }
                if ( cursor !in listOf(Cursor.N_RESIZE,Cursor.S_RESIZE) ) {
                    val minWidth = if (stage.minWidth > margin * 2) stage.minWidth else (margin * 2).toDouble()
                    if ( cursor in listOf(Cursor.NW_RESIZE,Cursor.W_RESIZE,Cursor.SW_RESIZE) ) {
                        if (stage.width > minWidth || pointer.x < 0) {
                            stage.width = stage.x - event.screenX + stage.width
                            stage.x = event.screenX
                        }
                    } else {
                        if (stage.width > minWidth || pointer.x + position.x - stage.width > 0) {
                            stage.width = pointer.x + position.x
                        }
                    }
                }
            }
        }
    }
}