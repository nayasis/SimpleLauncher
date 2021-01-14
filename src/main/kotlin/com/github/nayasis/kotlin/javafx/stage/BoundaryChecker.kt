package com.github.nayasis.kotlin.javafx.stage

import com.github.nayasis.kotlin.javafx.misc.Desktop
import com.github.nayasis.kotlin.javafx.model.Point
import javafx.stage.Stage
import mu.KotlinLogging
import java.awt.GraphicsConfiguration
import java.io.Serializable
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import java.awt.Insets as AwtInsects

private val log = KotlinLogging.logger {}

object BoundaryChecker {

    /**
     * reset stage position to displayed screen
     *
     * @param stage Stage
     */
    fun reset(stage: Stage) {
        if( screens().isEmpty() ) return
        if( getScreenHavingStart(stage) == null ) {
            val point = defaultPoint()
            stage.x = point.x
            stage.y = point.y
        }
    }

    private fun defaultPoint(): Point = Desktop.graphics().maximumWindowBounds.let { Point(it.x,it.y) }

    private fun getScreenHavingStart(stage: Stage): Inset? {
        for( screen in screens() ) {
            if( screen.hasStart(stage) ) return screen
        }
        return null
    }

    private fun getScreenHavingMaxArea(stage: Stage): Inset? {

        val window = Inset(stage)
        var maxArea = Int.MIN_VALUE
        var maxScreen: Inset? = null

        for( screen in screens() ) {
            if(screen.exclusive(window) ) continue
            val area = screen.intersectedArea(window)
            if( maxArea <= area ) {
                maxArea = area
                maxScreen = screen
            }
        }
        return maxScreen

    }

    private fun screens(): List<Inset> {
        return try {
            Desktop.graphics().screenDevices.map { Inset(it.defaultConfiguration) }
        } catch (e: Exception) {
            log.error(e.message, e)
            emptyList()
        }
    }

}

private data class Inset (
    val top: Int,
    val left: Int,
    val right: Int,
    val bottom: Int,
): Serializable {

    constructor(stage: Stage): this(
        stage.y.toInt(),
        stage.x.toInt(),
        (stage.x + stage.width).toInt(),
        (stage.y + stage.height).toInt()
    )
    constructor(insets: AwtInsects): this(
        insets.top,
        insets.left,
        insets.right,
        insets.bottom,
    )
    constructor(config: GraphicsConfiguration): this(Desktop.toolkit().getScreenInsets(config))

    fun width(): Int = abs( right - left )
    fun height(): Int = abs( bottom - top )
    fun area(): Int = width() * height()

    private fun topIntersected(other: Inset): Boolean {
        return other.top in top..bottom
            || top in other.top..other.bottom
    }

    private fun leftIntersected(other: Inset): Boolean {
        return other.left in left..right
            || left in other.left..other.right
    }

    private fun bottomIntersected(other: Inset): Boolean {
        return other.bottom in top..bottom
            || bottom in other.top..other.bottom
    }

    private fun rightIntersected(other: Inset): Boolean {
        return other.right in left..right
            || right in other.left..other.right
    }

    fun hasStart(stage: Stage): Boolean {
        return hasStart(Inset(stage))
    }

    fun hasStart(other: Inset): Boolean {
        return other.top in top..bottom && other.left in left..right
    }

    fun exclusive(other: Inset): Boolean {
        return ! topIntersected(other) && ! leftIntersected(other) && ! rightIntersected(other) && ! bottomIntersected(other)
    }

    fun intersectedArea(other: Inset): Int {
        return when {
            exclusive(other) -> 0
            else -> {
                Inset(
                    max(this.top, other.top),
                    max(this.left, other.left),
                    min(this.right, other.right),
                    min(this.bottom, other.bottom),
                ).area()
            }
        }
    }

}