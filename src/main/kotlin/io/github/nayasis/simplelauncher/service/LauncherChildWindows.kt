package io.github.nayasis.simplelauncher.service

import io.github.nayasis.kotlin.javafx.stage.progress.ProgressDialog
import io.github.nayasis.simplelauncher.common.Context
import io.github.nayasis.simplelauncher.view.Terminal
import javafx.application.Platform
import javafx.geometry.Rectangle2D
import javafx.stage.Screen
import javafx.stage.Stage
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
    private val progressDialogGap = 12.0

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

    private fun bindHiddenState(dialog: ProgressDialog) {
        dialog.stage.addEventHandler(WindowEvent.WINDOW_SHOWN) {
            placeProgressDialog(dialog)
            hideIfParentHidden(dialog)
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
        val screenBounds = Screen.getScreensForRectangle(
            mainStage.x,
            mainStage.y,
            mainStage.width.coerceAtLeast(1.0),
            mainStage.height.coerceAtLeast(1.0),
        ).firstOrNull()?.visualBounds ?: Screen.getPrimary().visualBounds

        val candidates = listOfNotNull(
            placeAbove(mainStage, dialogWidth, dialogHeight, screenBounds),
            placeRight(mainStage, dialogWidth, dialogHeight, screenBounds),
            placeLeft(mainStage, dialogWidth, dialogHeight, screenBounds),
            placeBelow(mainStage, dialogWidth, dialogHeight, screenBounds),
        )

        val position = candidates.firstOrNull() ?: fallbackBottomRight(mainStage, dialogWidth, dialogHeight, screenBounds)
        dialogStage.x = position.minX
        dialogStage.y = position.minY
    }

    private fun placeAbove(mainStage: Stage, dialogWidth: Double, dialogHeight: Double, bounds: Rectangle2D): Rectangle2D? {
        val y = mainStage.y - progressDialogGap - dialogHeight
        if(y < bounds.minY) return null
        val x = clamp(mainStage.x + (mainStage.width - dialogWidth) / 2, bounds.minX, bounds.maxX - dialogWidth)
        return Rectangle2D(x, y, dialogWidth, dialogHeight)
    }

    private fun placeRight(mainStage: Stage, dialogWidth: Double, dialogHeight: Double, bounds: Rectangle2D): Rectangle2D? {
        val x = mainStage.x + mainStage.width + progressDialogGap
        if(x + dialogWidth > bounds.maxX) return null
        val y = clamp(mainStage.y + (mainStage.height - dialogHeight) / 2, bounds.minY, bounds.maxY - dialogHeight)
        return Rectangle2D(x, y, dialogWidth, dialogHeight)
    }

    private fun placeLeft(mainStage: Stage, dialogWidth: Double, dialogHeight: Double, bounds: Rectangle2D): Rectangle2D? {
        val x = mainStage.x - progressDialogGap - dialogWidth
        if(x < bounds.minX) return null
        val y = clamp(mainStage.y + (mainStage.height - dialogHeight) / 2, bounds.minY, bounds.maxY - dialogHeight)
        return Rectangle2D(x, y, dialogWidth, dialogHeight)
    }

    private fun placeBelow(mainStage: Stage, dialogWidth: Double, dialogHeight: Double, bounds: Rectangle2D): Rectangle2D? {
        val y = mainStage.y + mainStage.height + progressDialogGap
        if(y + dialogHeight > bounds.maxY) return null
        val x = clamp(mainStage.x + (mainStage.width - dialogWidth) / 2, bounds.minX, bounds.maxX - dialogWidth)
        return Rectangle2D(x, y, dialogWidth, dialogHeight)
    }

    private fun fallbackBottomRight(mainStage: Stage, dialogWidth: Double, dialogHeight: Double, bounds: Rectangle2D): Rectangle2D {
        val x = clamp(mainStage.x + mainStage.width - dialogWidth, bounds.minX, bounds.maxX - dialogWidth)
        val y = clamp(mainStage.y + mainStage.height - dialogHeight, bounds.minY, bounds.maxY - dialogHeight)
        return Rectangle2D(x, y, dialogWidth, dialogHeight)
    }

    private fun clamp(value: Double, min: Double, max: Double): Double {
        if(max < min) return min
        return value.coerceIn(min, max)
    }

}
