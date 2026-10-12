package io.github.nayasis.simplelauncher.service

import io.github.nayasis.kotlin.swing.misc.isUiThread
import io.github.nayasis.kotlin.swing.misc.runLater
import io.github.nayasis.kotlin.swing.property.PositionProperty
import io.github.nayasis.kotlin.swing.window.BoundaryChecker
import io.github.nayasis.kotlin.swing.window.progress.ProgressDialog
import io.github.nayasis.kotlin.swing.window.visibleBounds
import io.github.nayasis.simplelauncher.common.Context
import io.github.nayasis.simplelauncher.view.Terminal
import java.awt.GraphicsEnvironment
import java.awt.Point
import java.awt.Rectangle
import java.awt.Window
import java.awt.event.ComponentAdapter
import java.awt.event.ComponentEvent
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent

/**
 * progress dialogs and terminals started by the launcher.
 *
 * they are hidden while the main window is hidden or minimized, and come back with it.
 * a window shown while the main window is hidden is hidden right away.
 */
internal class LauncherChildWindows {

    private val progressDialogs = LinkedHashSet<ProgressDialog>()
    private val hiddenProgressDialogs = LinkedHashSet<ProgressDialog>()
    private val progressQueuePopOvers = LinkedHashMap<ProgressDialog, ProgressQueuePopOver>()
    private val terminals = LinkedHashSet<Terminal>()
    private val hiddenTerminals = LinkedHashSet<Terminal>()
    @Volatile
    private var visible = true
    private val boundaryChecker = BoundaryChecker()
    private var lastProgressDialogPosition: Point? = loadSavedProgressDialogPosition()

    fun hide() {
        visible = false
        onUiThread {
            hideProgressDialogs()
            hideTerminals()
        }
    }

    fun restore() {
        visible = true
        onUiThread {
            restoreProgressDialogs()
            restoreTerminals()
        }
    }

    fun hasProgressDialogs(): Boolean {
        return synchronized(progressDialogs) {
            progressDialogs
                .filter { dialog -> !dialog.window.isShowing && dialog !in hiddenProgressDialogs }
                .forEach { dialog -> unregister(dialog) }
            progressDialogs.isNotEmpty()
        }
    }

    fun register(dialog: ProgressDialog, popOver: ProgressQueuePopOver) {
        synchronized(progressDialogs) {
            progressDialogs.add(dialog)
            progressQueuePopOvers[dialog] = popOver
        }
        dialog.window.addComponentListener(object: ComponentAdapter() {
            override fun componentShown(e: ComponentEvent) = hideIfParentHidden(dialog)
            override fun componentHidden(e: ComponentEvent) = rememberProgressDialogPosition(dialog)
        })
        hideIfParentHidden(dialog)
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
        terminal.addWindowListener(object: WindowAdapter() {
            override fun windowClosing(e: WindowEvent) = unregister(terminal)
            override fun windowClosed(e: WindowEvent) = unregister(terminal)
        })
        terminal.addComponentListener(object: ComponentAdapter() {
            override fun componentShown(e: ComponentEvent) = hideIfParentHidden(terminal)
        })
    }

    fun unregister(terminal: Terminal) {
        synchronized(terminals) {
            terminals.remove(terminal)
            hiddenTerminals.remove(terminal)
        }
    }

    fun show(terminal: Terminal) {
        terminal.isVisible = true
        hideIfParentHidden(terminal)
    }

    fun refreshProgressQueue(dialog: ProgressDialog) {
        progressQueuePopOvers[dialog]?.refreshIfShowing()
    }

    /** where to show [dialog]: where the last one was, or centered on the main window's screen. */
    fun progressDialogPosition(dialog: ProgressDialog): PositionProperty? {
        val mainFrame = runCatching { Context.main.primaryFrame }.getOrNull() ?: return null
        val size = dialog.window.size.takeIf { it.width > 0 && it.height > 0 } ?: dialog.window.preferredSize
        val position = resolveProgressDialogPosition(
            lastPosition = lastProgressDialogPosition,
            dialogWidth = size.width,
            dialogHeight = size.height,
            mainBounds = majorScreenBoundsOf(mainFrame),
            isShownOnScreen = boundaryChecker::isShownOnScreen,
        )
        lastProgressDialogPosition = position
        return PositionProperty(position.x, position.y)
    }

    private fun hideIfParentHidden(dialog: ProgressDialog) {
        if(visible) return
        synchronized(progressDialogs) {
            if(dialog !in progressDialogs) return
            hiddenProgressDialogs.add(dialog)
            progressQueuePopOvers[dialog]?.hide()
        }
        if(dialog.window.isShowing) {
            dialog.window.isVisible = false
        }
    }

    private fun hideIfParentHidden(terminal: Terminal) {
        if(visible) return
        trackTerminal(terminal)
        if(terminal.isShowing) {
            terminal.isVisible = false
        }
    }

    private fun hideProgressDialogs() {
        synchronized(progressDialogs) {
            progressDialogs.forEach { dialog ->
                if(dialog.window.isShowing) {
                    hiddenProgressDialogs.add(dialog)
                    progressQueuePopOvers[dialog]?.hide()
                    dialog.window.isVisible = false
                }
            }
        }
    }

    private fun restoreProgressDialogs() {
        synchronized(progressDialogs) {
            hiddenProgressDialogs.toList().forEach { dialog ->
                hiddenProgressDialogs.remove(dialog)
                if(dialog !in progressDialogs) return@forEach
                runCatching {
                    if(!dialog.window.isShowing) dialog.show()
                }.onFailure {
                    unregister(dialog)
                }
            }
        }
    }

    private fun hideTerminals() {
        currentTerminals().forEach { terminal ->
            trackTerminal(terminal)
            if(terminal.isShowing) {
                terminal.isVisible = false
            }
        }
    }

    private fun restoreTerminals() {
        synchronized(terminals) {
            hiddenTerminals.toList().forEach { terminal ->
                hiddenTerminals.remove(terminal)
                if(terminal !in terminals || !terminal.isDisplayable) return@forEach
                runCatching {
                    if(!terminal.isShowing) terminal.isVisible = true
                }.onFailure {
                    unregister(terminal)
                }
            }
        }
    }

    private fun currentTerminals(): List<Terminal> {
        return synchronized(terminals) {
            val openTerminals = Window.getWindows().filterIsInstance<Terminal>().filter { it.isDisplayable }
            (terminals + openTerminals).distinct()
        }
    }

    private fun trackTerminal(terminal: Terminal) {
        synchronized(terminals) {
            terminals.add(terminal)
            hiddenTerminals.add(terminal)
        }
    }

    private fun onUiThread(block: () -> Unit) {
        if(isUiThread) block() else runLater(block)
    }

    private fun rememberProgressDialogPosition(dialog: ProgressDialog) {
        // hiding along with the main window is not a move by the user
        if(!visible) return
        val position = dialog.window.location
        lastProgressDialogPosition = position
        runCatching {
            val config = Context.config
            config.progressDialogX = position.x.toDouble()
            config.progressDialogY = position.y.toDouble()
            config.save()
        }
    }

    private fun majorScreenBoundsOf(window: Window): Rectangle {
        return runCatching { boundaryChecker.getMajorScreen(window).visibleBounds }
            .getOrElse { GraphicsEnvironment.getLocalGraphicsEnvironment().defaultScreenDevice.defaultConfiguration.visibleBounds }
    }

    private fun loadSavedProgressDialogPosition(): Point? {
        val x = Context.config.progressDialogX
        val y = Context.config.progressDialogY
        if(x == null || y == null) return null
        if(!x.isFinite() || !y.isFinite()) return null
        return Point(x.toInt(), y.toInt())
    }

}

internal fun resolveProgressDialogPosition(
    lastPosition: Point?,
    dialogWidth: Int,
    dialogHeight: Int,
    mainBounds: Rectangle,
    isShownOnScreen: (Rectangle) -> Boolean,
): Point {
    if(dialogWidth <= 0 || dialogHeight <= 0) {
        return Point(mainBounds.x, mainBounds.y)
    }

    val savedPosition = lastPosition?.takeIf {
        isShownOnScreen(Rectangle(it.x, it.y, dialogWidth, dialogHeight))
    }
    if(savedPosition != null) {
        return savedPosition
    }

    val x = mainBounds.x + (mainBounds.width - dialogWidth) / 2
    val y = mainBounds.y + (mainBounds.height - dialogHeight) / 2
    return Point(x, y)
}
