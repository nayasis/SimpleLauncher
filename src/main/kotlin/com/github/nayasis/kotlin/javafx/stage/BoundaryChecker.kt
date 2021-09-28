package com.github.nayasis.kotlin.javafx.stage

import javafx.geometry.Rectangle2D
import javafx.scene.control.Dialog
import javafx.stage.Screen
import javafx.stage.Window
import mu.KotlinLogging
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

private val log = KotlinLogging.logger {}

object BoundaryChecker {

    /**
     * reset stage position to displayed screen
     *
     * @param window Window
     */
    fun reset(window: Window) {
        if( getScreenContains(window.x,window.y) == null ) {
            Screen.getPrimary().visualBounds.let {
                window.x = it.minX
                window.y = it.minY
                window.width  = min(window.width,  it.width)
                window.height = min(window.height, it.height)
            }
        }
    }

    /**
     * reset stage position to displayed screen
     *
     * @param dialog Window
     */
    fun reset(dialog: Dialog<*>) {
        if( getScreenContains(dialog.x,dialog.y) == null ) {
            Screen.getPrimary().visualBounds.let {
                dialog.x = it.minX
                dialog.y = it.minY
                dialog.width  = min(dialog.width,  it.width)
                dialog.height = min(dialog.height, it.height)
            }
        }
    }

    /**
     * get screen which contains start point(x,y)
     */
    fun getScreenContains(x: Double, y: Double): Screen? {
        return Screen.getScreens().firstOrNull{ it.bounds.contains(x,y) }
    }

    /**
     * get screen which has maximum stage area potion.
     *
     * if there is no screen related to stage, return primary screen.
     */
    fun getMajorScreen(window: Window): Screen {

        var major = Screen.getPrimary()
        var max   = 0.0

        val boundary = window.boundary

        for( screen in Screen.getScreens() ) {
            val area = screen.bounds.areaIntersected(boundary)
            if( area > max ) {
                major = screen
                max = area
            }
        }

        return major

    }

}

private inline fun Rectangle2D.areaIntersected(r: Rectangle2D): Double {
    return when {
        this.contains(r) -> this.width * this.height
        this.intersects(r) -> {
            val minX = max(this.minX,r.minX)
            val maxX = min(this.maxX,r.maxX)
            val minY = max(this.minY,r.minY)
            val maxY = min(this.maxY,r.maxY)
            abs(minX - maxX) * abs(minY - maxY)
        }
        else -> 0.0
    }
}