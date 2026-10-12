package io.github.nayasis.simplelauncher.service

import io.github.nayasis.kotlin.basica.core.string.message
import io.github.nayasis.kotlin.basica.exec.CommandExecutor
import io.github.nayasis.kotlin.swing.misc.runLater
import io.github.nayasis.kotlin.swing.window.PopOver
import java.awt.BorderLayout
import java.awt.Color
import java.awt.Component
import java.awt.Dimension
import java.awt.Font
import java.awt.Insets
import javax.swing.BorderFactory
import javax.swing.BoxLayout
import javax.swing.JButton
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JScrollPane
import javax.swing.ScrollPaneConstants
import javax.swing.UIManager
import java.io.File

private const val MAX_VISIBLE_ITEMS = 20
private const val MAX_ITEMS_HEIGHT = 600
private const val POPOVER_WIDTH = 280
private val CURRENT_ITEM_COLOR = Color(0x2f6fda)

private fun messageOr(key: String, fallback: String): String {
    return key.message().takeIf { it != key } ?: fallback
}

internal data class ProgressFileQueueItem(
    val file: File,
) {
    val title: String
        get() = file.name
}

internal class ProgressFileQueue(files: Collection<File>) {

    private val items = ArrayList(files.map { ProgressFileQueueItem(it) })
    private var current: ProgressFileQueueItem? = null
    private var currentNo = 0

    val currNo: Int
        @Synchronized get() = currentNo

    val size: Int
        @Synchronized get() = items.size

    @Synchronized
    fun hasNext(): Boolean {
        return currentNo < items.size
    }

    @Synchronized
    fun next(): ProgressFileQueueItem? {
        return if (hasNext()) {
            items[currentNo++].also { current = it }
        } else {
            null
        }
    }

    @Synchronized
    fun currentItem(): ProgressFileQueueItem? {
        return current
    }

    @Synchronized
    fun finish(item: ProgressFileQueueItem) {
        if (current == item) {
            current = null
        }
    }

    @Synchronized
    fun pendingItems(): List<ProgressFileQueueItem> {
        return if (currentNo >= items.size) {
            emptyList()
        } else {
            items.subList(currentNo, items.size).toList()
        }
    }

    @Synchronized
    fun removePending(item: ProgressFileQueueItem): Boolean {
        val index = items.indexOf(item)
        return if (index >= currentNo) {
            items.removeAt(index)
            true
        } else {
            false
        }
    }
}

internal class ProgressQueueControl(
    private val queue: ProgressFileQueue,
    private val onChanged: () -> Unit,
) {

    @Volatile
    private var executor: CommandExecutor? = null

    @Volatile
    private var cancellingItem: ProgressFileQueueItem? = null

    fun updateExecutor(executor: CommandExecutor?) {
        this.executor = executor
        if (executor == null) {
            cancellingItem = null
        }
        onChanged()
    }

    fun canCancel(item: ProgressFileQueueItem): Boolean {
        return queue.currentItem() == item && executor != null
    }

    fun isCancelling(item: ProgressFileQueueItem): Boolean {
        return cancellingItem == item
    }

    fun cancel(item: ProgressFileQueueItem): Boolean {
        if (!canCancel(item)) return false
        cancellingItem = item
        executor?.destroy()
        onChanged()
        return true
    }
}

internal class ProgressQueuePopOver(
    private val items: ProgressFileQueue,
    private val control: ProgressQueueControl,
    private val onPendingRemoved: () -> Unit,
) {

    private val root = JPanel(BorderLayout()).apply {
        border = BorderFactory.createEmptyBorder(5, 5, 5, 10)
    }

    private val popOver = PopOver(root)

    val isShowing: Boolean
        get() = popOver.isShowing

    fun toggle(anchor: Component) {
        if (isShowing) hide() else show(anchor)
    }

    fun show(anchor: Component) {
        render()
        popOver.show(anchor)
    }

    fun refresh() {
        render()
        popOver.repack()
    }

    fun refreshIfShowing() {
        runLater {
            if (isShowing) refresh()
        }
    }

    fun hide() {
        popOver.hide()
    }

    private fun render() {
        val current = items.currentItem()
        val pendingItems = items.pendingItems()
        val visibleItemCount = pendingItems.size + if (current == null) 0 else 1
        val itemList = JPanel().apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
            current?.let { add(currentRow(it)) }
            if (pendingItems.isEmpty()) {
                if (current == null) {
                    add(row(JLabel(messageOr("label.work.queue.pending.empty", "대기 중인 작업이 없습니다.")), null))
                }
            } else {
                pendingItems.forEach { add(pendingRow(it)) }
            }
        }
        root.removeAll()
        if (visibleItemCount <= MAX_VISIBLE_ITEMS) {
            root.add(itemList, BorderLayout.CENTER)
        } else {
            root.add(JScrollPane(itemList).apply {
                border = BorderFactory.createEmptyBorder()
                horizontalScrollBarPolicy = ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER
                verticalScrollBar.unitIncrement = 16
                preferredSize = Dimension(POPOVER_WIDTH, MAX_ITEMS_HEIGHT)
            }, BorderLayout.CENTER)
        }
        root.preferredSize = null
        root.preferredSize = Dimension(POPOVER_WIDTH, root.preferredSize.height)
        root.revalidate()
        root.repaint()
    }

    private fun currentRow(item: ProgressFileQueueItem): JComponent {
        val label = JLabel(item.title).apply {
            foreground = UIManager.getColor("Component.accentColor") ?: CURRENT_ITEM_COLOR
            font = font.deriveFont(Font.BOLD)
        }
        val button = actionButton(messageOr("btn.cancel", "취소")).apply {
            isEnabled = control.canCancel(item) && !control.isCancelling(item)
            addActionListener {
                if (control.cancel(item)) isEnabled = false
            }
        }
        return row(label, button)
    }

    private fun pendingRow(item: ProgressFileQueueItem): JComponent {
        val button = actionButton(messageOr("btn.delete", "삭제")).apply {
            addActionListener {
                if (items.removePending(item)) {
                    isEnabled = false
                    onPendingRemoved()
                    refreshIfShowing()
                }
            }
        }
        return row(JLabel(item.title), button)
    }

    private fun row(label: JLabel, button: JButton?): JComponent {
        return JPanel(BorderLayout(4, 0)).apply {
            isOpaque = false
            border = BorderFactory.createEmptyBorder(1, 0, 1, 0)
            add(label, BorderLayout.CENTER)
            button?.let { add(it, BorderLayout.EAST) }
            alignmentX = Component.LEFT_ALIGNMENT
            maximumSize = Dimension(Int.MAX_VALUE, preferredSize.height)
        }
    }

    private fun actionButton(text: String) = JButton(text).apply {
        isFocusable = false
        margin = Insets(1, 8, 1, 8)
        font = font.deriveFont(font.size2D * 0.9f)
    }

}

