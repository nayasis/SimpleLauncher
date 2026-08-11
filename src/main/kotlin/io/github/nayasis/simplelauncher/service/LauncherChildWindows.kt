package io.github.nayasis.simplelauncher.service

import io.github.nayasis.kotlin.javafx.stage.BoundaryChecker
import io.github.nayasis.kotlin.javafx.property.PositionProperty
import io.github.nayasis.kotlin.javafx.stage.progress.ProgressDialog
import io.github.nayasis.simplelauncher.common.Context
import io.github.nayasis.simplelauncher.view.Terminal
import javafx.application.Platform
import javafx.geometry.Dimension2D
import javafx.geometry.Point2D
import javafx.geometry.Rectangle2D
import javafx.scene.Parent
import javafx.stage.Screen
import javafx.stage.Window
import javafx.stage.WindowEvent
import tornadofx.runLater

internal class LauncherChildWindows {

    private val progressDialogs = LinkedHashSet<ProgressDialog>()
    private val hiddenProgressDialogs = LinkedHashSet<ProgressDialog>()
    private val progressQueuePopOvers = LinkedHashMap<ProgressDialog, ProgressQueuePopOver>()
    private val terminals = LinkedHashSet<Terminal>()
    private val hiddenTerminals = LinkedHashSet<Terminal>()
    private var visible = true
    private val boundaryChecker = BoundaryChecker()
    private var lastProgressDialogPosition: Point2D? = loadSavedProgressDialogPosition()

    fun hide() {
        synchronized(this) {
            visible = false
        }
        runOnFxThread {
            hideProgressDialogs()
            hideTerminals()
        }
    }

    fun restore() {
        synchronized(this) {
            visible = true
        }
        runOnFxThread {
            restoreProgressDialogs()
            restoreTerminals()
        }
    }

    fun hasProgressDialogs(): Boolean {
        return synchronized(progressDialogs) {
            progressDialogs
                .filter { dialog -> !dialog.stage.isShowing && dialog !in hiddenProgressDialogs }
                .forEach { dialog -> unregister(dialog) }
            progressDialogs.isNotEmpty()
        }
    }

    fun register(dialog: ProgressDialog) {
        synchronized(progressDialogs) {
            progressDialogs.add(dialog)
        }
        bindHiddenState(dialog)
    }

    fun register(dialog: ProgressDialog, popOver: ProgressQueuePopOver) {
        synchronized(progressDialogs) {
            progressDialogs.add(dialog)
            progressQueuePopOvers[dialog] = popOver
        }
        bindHiddenState(dialog)
    }

    fun unregister(dialog: ProgressDialog) {
        synchronized(progressDialogs) {
            progressDialogs.remove(dialog)
            hiddenProgressDialogs.remove(dialog)
            progressQueuePopOvers.remove(dialog)?.hide()
        }
    }

    fun register(terminal: Terminal) {
        synchronized(terminals) {
            terminals.add(terminal)
        }
        terminal.addEventHandler(WindowEvent.WINDOW_CLOSE_REQUEST) {
            unregister(terminal)
        }
        terminal.addEventHandler(WindowEvent.WINDOW_SHOWN) {
            hideIfParentHidden(terminal)
        }
    }

    fun unregister(terminal: Terminal) {
        synchronized(terminals) {
            terminals.remove(terminal)
            hiddenTerminals.remove(terminal)
        }
    }

    fun show(terminal: Terminal) {
        terminal.show()
        hideIfParentHidden(terminal)
    }

    fun hideProgressQueue(dialog: ProgressDialog) {
        progressQueuePopOvers[dialog]?.hide()
    }

    fun refreshProgressQueue(dialog: ProgressDialog) {
        progressQueuePopOvers[dialog]?.refreshIfShowing()
    }

    fun progressDialogPosition(dialog: ProgressDialog): PositionProperty? {
        val mainStage = Context.main.currentStage ?: return null
        val dialogSize = measureProgressDialog(dialog) ?: return null
        val mainBounds = majorScreenBoundsOf(mainStage)
        val position = resolveProgressDialogPosition(
            lastPosition = lastProgressDialogPosition,
            dialogWidth = dialogSize.width,
            dialogHeight = dialogSize.height,
            mainBounds = mainBounds,
            isShownOnScreen = boundaryChecker::isShownOnScreen,
        )
        return PositionProperty(position.x.toInt(), position.y.toInt())
    }

    private fun bindHiddenState(dialog: ProgressDialog) {
        dialog.stage.addEventHandler(WindowEvent.WINDOW_SHOWN) {
            placeProgressDialog(dialog)
            hideIfParentHidden(dialog)
        }
        dialog.stage.addEventHandler(WindowEvent.WINDOW_HIDDEN) {
            rememberProgressDialogPosition(dialog)
        }
        hideIfParentHidden(dialog)
    }

    private fun hideIfParentHidden(dialog: ProgressDialog) {
        if(isVisible()) return
        synchronized(progressDialogs) {
            if(dialog !in progressDialogs) return
            hiddenProgressDialogs.add(dialog)
            hideProgressQueue(dialog)
        }
        if(dialog.stage.isShowing) {
            dialog.stage.hide()
        }
    }

    private fun hideIfParentHidden(terminal: Terminal) {
        if(isVisible()) return
        trackTerminal(terminal)
        if(terminal.isShowing) {
            terminal.hide()
        }
    }

    private fun hideProgressDialogs() {
        synchronized(progressDialogs) {
            progressDialogs.forEach { dialog ->
                if(dialog.stage.isShowing) {
                    hiddenProgressDialogs.add(dialog)
                    hideProgressQueue(dialog)
                    dialog.stage.hide()
                }
            }
        }
    }

    private fun restoreProgressDialogs() {
        synchronized(progressDialogs) {
            hiddenProgressDialogs.toList().forEach { dialog ->
                if(dialog !in progressDialogs) {
                    hiddenProgressDialogs.remove(dialog)
                    return@forEach
                }
                runCatching {
                    if(!dialog.stage.isShowing) {
                        dialog.show()
                    }
                }.onFailure {
                    unregister(dialog)
                }
                hiddenProgressDialogs.remove(dialog)
            }
        }
    }

    private fun hideTerminals() {
        currentTerminals().forEach { terminal ->
            trackTerminal(terminal)
            if(terminal.isShowing) {
                terminal.hide()
            }
        }
    }

    private fun restoreTerminals() {
        synchronized(terminals) {
            hiddenTerminals.toList().forEach { terminal ->
                if(terminal !in terminals) {
                    hiddenTerminals.remove(terminal)
                    return@forEach
                }
                runCatching {
                    if(!terminal.isShowing) {
                        terminal.show()
                    }
                }.onFailure {
                    unregister(terminal)
                }
                hiddenTerminals.remove(terminal)
            }
        }
    }

    private fun isVisible(): Boolean {
        return synchronized(this) {
            visible
        }
    }

    private fun currentTerminals(): List<Terminal> {
        return synchronized(terminals) {
            val openTerminals = Window.getWindows().filterIsInstance<Terminal>()
            (terminals + openTerminals).distinct()
        }
    }

    private fun trackTerminal(terminal: Terminal) {
        synchronized(terminals) {
            terminals.add(terminal)
            hiddenTerminals.add(terminal)
        }
    }

    private fun runOnFxThread(block: () -> Unit) {
        if(Platform.isFxApplicationThread()) {
            block()
        } else {
            runLater {
                block()
            }
        }
    }

    private fun placeProgressDialog(dialog: ProgressDialog) {
        val mainStage = Context.main.currentStage ?: return
        val dialogStage = dialog.stage
        val dialogWidth = dialogStage.width.takeIf { it > 0 } ?: return
        val dialogHeight = dialogStage.height.takeIf { it > 0 } ?: return

        val position = resolveProgressDialogPosition(
            lastPosition = lastProgressDialogPosition,
            dialogWidth = dialogWidth,
            dialogHeight = dialogHeight,
            mainBounds = majorScreenBoundsOf(mainStage),
            isShownOnScreen = boundaryChecker::isShownOnScreen,
        )

        dialogStage.x = position.x
        dialogStage.y = position.y
        lastProgressDialogPosition = position
    }

    private fun rememberProgressDialogPosition(dialog: ProgressDialog) {
        val x = dialog.stage.x
        val y = dialog.stage.y
        if(!x.isFinite() || !y.isFinite()) return
        val position = Point2D(x, y)
        lastProgressDialogPosition = position
        saveProgressDialogPosition(position)
    }

    private fun measureProgressDialog(dialog: ProgressDialog): Dimension2D? {
        val stage = dialog.stage
        val root = stage.scene?.root as? Parent ?: return null
        root.applyCss()
        root.autosize()
        val width = stage.width.takeIf { it > 0.0 }
            ?: root.prefWidth(-1.0).takeIf { it > 0.0 }
            ?: root.layoutBounds.width.takeIf { it > 0.0 }
            ?: return null
        val height = stage.height.takeIf { it > 0.0 }
            ?: root.prefHeight(width).takeIf { it > 0.0 }
            ?: root.layoutBounds.height.takeIf { it > 0.0 }
            ?: return null
        return Dimension2D(width, height)
    }

    private fun majorScreenBoundsOf(window: Window): Rectangle2D {
        return runCatching { boundaryChecker.getMajorScreen(window).visualBounds }
            .getOrElse { Screen.getPrimary().visualBounds }
    }

    private fun saveProgressDialogPosition(position: Point2D) {
        runCatching {
            val config = Context.config
            config.progressDialogX = position.x
            config.progressDialogY = position.y
            config.save()
        }
    }

    private fun loadSavedProgressDialogPosition(): Point2D? {
        val x = Context.config.progressDialogX
        val y = Context.config.progressDialogY
        if(x == null || y == null) return null
        if(!x.isFinite() || !y.isFinite()) return null
        return Point2D(x, y)
    }

}

internal fun resolveProgressDialogPosition(
    lastPosition: Point2D?,
    dialogWidth: Double,
    dialogHeight: Double,
    mainBounds: Rectangle2D,
    isShownOnScreen: (Rectangle2D) -> Boolean,
): Point2D {
    if(dialogWidth <= 0.0 || dialogHeight <= 0.0) {
        return Point2D(mainBounds.minX, mainBounds.minY)
    }

    val savedPosition = lastPosition?.takeIf {
        isShownOnScreen(Rectangle2D(it.x, it.y, dialogWidth, dialogHeight))
    }
    if(savedPosition != null) {
        return savedPosition
    }

    val x = mainBounds.minX + (mainBounds.width - dialogWidth) / 2.0
    val y = mainBounds.minY + (mainBounds.height - dialogHeight) / 2.0
    return Point2D(x, y)
}
