package io.github.nayasis.simplelauncher.service

import io.github.nayasis.kotlin.basica.core.string.message
import io.github.nayasis.kotlin.basica.exec.CommandExecutor
import javafx.geometry.Insets
import javafx.geometry.Pos
import javafx.scene.Node
import javafx.scene.control.Button
import javafx.scene.control.Label
import javafx.scene.control.ScrollPane
import javafx.scene.layout.HBox
import javafx.scene.layout.Priority
import javafx.scene.layout.Region
import javafx.scene.layout.VBox
import org.controlsfx.control.PopOver
import tornadofx.runLater
import java.io.File

private const val MAX_VISIBLE_ITEMS = 20
private const val MAX_ITEMS_HEIGHT = 600.0
private const val ACTION_BUTTON_WIDTH = 48.0

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

    private var root: VBox? = null
    private var popOver: PopOver? = null

    val isShowing: Boolean
        get() = popOver?.isShowing == true

    fun toggle(anchor: Node) {
        if (isShowing) {
            hide()
        } else {
            show(anchor)
        }
    }

    fun show(anchor: Node) {
        val popOver = ensurePopOver()
        refresh()
        if (!popOver.isShowing) {
            popOver.show(anchor)
        }
    }

    fun refresh() {
        root?.let { render(it) }
    }

    fun refreshIfShowing() {
        runLater {
            if (isShowing) {
                refresh()
            }
        }
    }

    fun hide() {
        popOver?.let { popOver ->
            if (popOver.isShowing) {
                popOver.hide()
            }
        }
        clearPopup()
    }

    private fun ensurePopOver(): PopOver {
        popOver?.let { return it }

        val root = VBox().apply {
            style = """
                -fx-font-size: 10px;
                -fx-pref-width: 280;
                -fx-padding: 5 0 5 0;
            """.trimIndent()
        }

        val popOver = PopOver().apply {
            isAutoHide = false
            contentNode = root
            setOnHidden {
                clearPopup()
            }
        }

        this.root = root
        this.popOver = popOver
        return popOver
    }

    private fun clearPopup() {
        popOver = null
        root = null
    }

    private fun render(root: VBox) {
        val current = items.currentItem()
        val pendingItems = items.pendingItems()
        val visibleItemCount = pendingItems.size + if (current == null) 0 else 1
        val itemList = VBox().apply {
            spacing = 1.0
            padding = Insets(0.0, 10.0, 0.0, 5.0)
            current?.let { item ->
                children += currentRow(item)
            }
            if (pendingItems.isEmpty()) {
                if (current == null) {
                    children += Label(messageOr("label.work.queue.pending.empty", "대기 중인 작업이 없습니다."))
                }
            } else {
                pendingItems.forEach { item ->
                    children += pendingRow(item)
                }
            }
        }
        root.children.clear()
        if (visibleItemCount <= MAX_VISIBLE_ITEMS) {
            root.children += itemList
        } else {
            root.children += ScrollPane(itemList).apply {
                fitToWidthProperty().set(true)
                prefViewportHeight = MAX_ITEMS_HEIGHT
                maxHeight = MAX_ITEMS_HEIGHT
                hbarPolicy = ScrollPane.ScrollBarPolicy.NEVER
                vbarPolicy = ScrollPane.ScrollBarPolicy.AS_NEEDED
                style = """
                    -fx-background-color: transparent;
                    -fx-padding: 0;
                """.trimIndent()
            }
        }
    }

    private fun currentRow(item: ProgressFileQueueItem): HBox {
        return HBox().apply {
            alignment = Pos.CENTER_LEFT
            spacing = 4.0
            children += Label(item.title).apply {
                maxWidth = Double.MAX_VALUE
                style = "-fx-text-fill: #2f6fda; -fx-font-weight: bold;"
                HBox.setHgrow(this, Priority.ALWAYS)
            }
            children += Region().apply {
                HBox.setHgrow(this, Priority.ALWAYS)
            }
            children += Button(messageOr("btn.cancel", "취소")).apply {
                style = "-fx-padding: 1 8 1 8; -fx-min-height: 20px;"
                minWidth = ACTION_BUTTON_WIDTH
                prefWidth = ACTION_BUTTON_WIDTH
                maxWidth = ACTION_BUTTON_WIDTH
                isDisable = !control.canCancel(item) || control.isCancelling(item)
                setOnAction { event ->
                    event.consume()
                    val cancelled = control.cancel(item)
                    if (cancelled) {
                        isDisable = true
                    }
                }
            }
        }
    }

    private fun pendingRow(item: ProgressFileQueueItem): HBox {
        return HBox().apply {
            alignment = Pos.CENTER_LEFT
            spacing = 4.0
            children += Label(item.title).apply {
                maxWidth = Double.MAX_VALUE
                HBox.setHgrow(this, Priority.ALWAYS)
            }
            children += Region().apply {
                HBox.setHgrow(this, Priority.ALWAYS)
            }
            children += Button(messageOr("btn.delete", "삭제")).apply {
                style = "-fx-padding: 1 8 1 8; -fx-min-height: 20px;"
                minWidth = ACTION_BUTTON_WIDTH
                prefWidth = ACTION_BUTTON_WIDTH
                maxWidth = ACTION_BUTTON_WIDTH
                setOnAction { event ->
                    event.consume()
                    val removed = items.removePending(item)
                    if (removed) {
                        isDisable = true
                        onPendingRemoved()
                        refreshIfShowing()
                    }
                }
            }
        }
    }
}
