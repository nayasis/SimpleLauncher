package io.github.nayasis.simplelauncher.view

import io.github.nayasis.kotlin.basica.core.string.message
import io.github.nayasis.simplelauncher.service.ShortcutAction
import io.github.nayasis.simplelauncher.service.ShortcutGroup
import io.github.nayasis.simplelauncher.service.ShortcutSettings
import io.github.nayasis.simplelauncher.service.findDuplicateShortcuts
import io.github.nayasis.simplelauncher.service.toShortcutCombination
import javafx.event.ActionEvent
import javafx.geometry.Insets
import javafx.scene.Node
import javafx.scene.Scene
import javafx.scene.control.Alert
import javafx.scene.control.Button
import javafx.scene.control.Label
import javafx.scene.control.ScrollPane
import javafx.scene.control.TextField
import javafx.scene.input.KeyCombination
import javafx.scene.input.KeyEvent.KEY_PRESSED
import javafx.scene.layout.BorderPane
import javafx.scene.layout.ColumnConstraints
import javafx.scene.layout.GridPane
import javafx.scene.layout.HBox
import javafx.scene.layout.Priority
import javafx.scene.layout.Region
import javafx.scene.layout.VBox
import javafx.stage.Modality
import javafx.stage.Stage
import tornadofx.View
import java.util.*

class ShortcutEditor: View("shortcut.dialog.title".message()) {

    private val duplicateStyleClasses = listOf(
        "shortcut-duplicate-1",
        "shortcut-duplicate-2",
        "shortcut-duplicate-3",
        "shortcut-duplicate-4",
        "shortcut-duplicate-5",
        "shortcut-duplicate-6",
        "shortcut-duplicate-7",
    )

    private val stylesheetUrl = ShortcutEditor::class.java.getResource("/view/main/shortcut-dialog.css")!!.toExternalForm()
    private val bindings      = EnumMap<ShortcutAction, KeyCombination>(ShortcutAction::class.java)
    private val editors       = EnumMap<ShortcutAction, TextField>(ShortcutAction::class.java)

    private var result: ShortcutSettings? = null
    private var dialogStage: Stage? = null

    private val contentScroll = ScrollPane().apply {
        styleClass        += "shortcut-dialog-scroll"
        isFitToWidth       = true
        prefViewportWidth  = 730.0
        prefViewportHeight = 520.0
        padding            = Insets.EMPTY
    }

    private val resetAllButton = Button("btn.reset.all".message()).apply {
        setOnAction {
            bindings.clear()
            ShortcutAction.entries.forEach { action ->
                bindings[action] = action.defaultCombination
            }
            editors.forEach { (action, editor) ->
                editor.text = bindings[action]!!.displayText
            }
            refreshDuplicateHighlighting()
        }
    }

    private val saveButton = Button("btn.save".message()).apply {
        addEventFilter(ActionEvent.ACTION) { event ->
            val duplicates = findDuplicateShortcuts(bindings)
            if (duplicates.isNotEmpty()) {
                showDuplicateAlert(duplicates)
                event.consume()
            } else {
                result = ShortcutSettings.fromBindings(bindings)
                dialogStage?.close()
            }
        }
    }

    private val cancelButton = Button("btn.cancel".message()).apply {
        setOnAction {
            dialogStage?.close()
        }
    }

    override val root = BorderPane().apply {
        styleClass  += "shortcut-dialog-root"
        stylesheets += stylesheetUrl
        center       = contentScroll
        bottom       = HBox(8.0).apply {
            styleClass += "shortcut-dialog-footer"
            children   += resetAllButton
            children   += Region().apply {
                HBox.setHgrow(this, Priority.ALWAYS)
            }
            children += saveButton
            children += cancelButton
        }
    }

    fun showDialog(owner: Stage?, shortcuts: ShortcutSettings): ShortcutSettings? {
        result = null
        editors.clear()
        bindings.clear()
        bindings.putAll(shortcuts.toMutableBindings())
        contentScroll.content = buildContent()
        refreshDuplicateHighlighting()

        val stage = Stage().apply {
            title = this@ShortcutEditor.title
            if(owner != null) {
                initOwner(owner)
                initModality(Modality.WINDOW_MODAL)
            } else {
                initModality(Modality.APPLICATION_MODAL)
            }
            minWidth = 760.0
            minHeight = 620.0
            scene = Scene(root)
        }

        dialogStage = stage
        stage.showAndWait()
        dialogStage = null
        return result
    }

    private fun buildContent(): VBox {
        return VBox().apply {
            styleClass += "shortcut-dialog-content"
            children += buildHelpBox()
            ShortcutGroup.entries.forEach { group ->
                children += buildGroupTitle(group.messageKey.message())
                children += buildGroup(group)
            }
        }
    }

    private fun buildGroupTitle(text: String): Label {
        return Label(text).apply {
            styleClass += "shortcut-group-title"
        }
    }

    private fun buildHelpBox(): HBox {
        return HBox().apply {
            styleClass += "shortcut-help-box"
            children += Label("i").apply {
                styleClass += "shortcut-help-icon"
            }
            children += Label("shortcut.dialog.help".message()).apply {
                styleClass += "shortcut-help-text"
                isWrapText = true
                maxWidth = Double.MAX_VALUE
                HBox.setHgrow(this, Priority.ALWAYS)
            }
        }
    }

    private fun buildGroup(group: ShortcutGroup): GridPane {
        return GridPane().apply {
            hgap = 1.0
            vgap = 8.0
            padding = Insets(0.0, 0.0, 6.0, 4.0)
            columnConstraints.addAll(
                ColumnConstraints().apply { prefWidth = 162.0 },
                ColumnConstraints().apply {
                    hgrow = Priority.ALWAYS
                    minWidth = 170.0
                },
                ColumnConstraints().apply { prefWidth = 98.0 },
                ColumnConstraints().apply { prefWidth = 70.0 },
            )

            addGroupCell(Label("shortcut.column.action".message()), 0, 0)
            addGroupCell(Label("shortcut.column.current".message()), 1, 0)
            addGroupCell(Label("shortcut.column.default".message()), 2, 0)

            var row = 1
            ShortcutAction.entries
                .filter { it.group == group }
                .forEach { action ->
                    addGroupCell(Label(action.messageKey.message()), 0, row)
                    addGroupCell(createEditor(action), 1, row)
                    addGroupCell(Label(action.defaultCombination.displayText), 2, row)
                    addGroupCell(Button("btn.reset".message()).apply {
                        setOnAction {
                            bindings[action] = action.defaultCombination
                            editors[action]?.text = action.defaultCombination.displayText
                            refreshDuplicateHighlighting()
                        }
                    }, 3, row)
                    row++
                }
        }
    }

    private fun createEditor(action: ShortcutAction): TextField {
        return TextField(bindings[action]?.displayText ?: action.defaultCombination.displayText).apply {
            id = "shortcut-${action.name}"
            isEditable = false
            isFocusTraversable = true
            editors[action] = this
            addEventFilter(KEY_PRESSED) { event ->
                event.consume()
                event.toShortcutCombination()?.let { combination ->
                    bindings[action] = combination
                    text = combination.displayText
                    refreshDuplicateHighlighting()
                }
            }
        }
    }

    private fun refreshDuplicateHighlighting() {
        val duplicateActionsByStyle = findDuplicateShortcuts(bindings)
            .mapIndexed { index, actions ->
                duplicateStyleClasses[index % duplicateStyleClasses.size] to actions.toSet()
            }

        editors.forEach { (action, editor) ->
            editor.styleClass.removeAll(duplicateStyleClasses)
            duplicateActionsByStyle.firstOrNull { (_, actions) -> action in actions }?.let { (styleClass, _) ->
                editor.styleClass += styleClass
            }
        }
    }

    private fun showDuplicateAlert(duplicates: List<List<ShortcutAction>>) {
        val labels = duplicates.joinToString("\n") { group ->
            group.joinToString(", ") { action -> action.messageKey.message() }
        }

        Alert(Alert.AlertType.ERROR).apply {
            title = this@ShortcutEditor.title
            headerText  = "msg.error.shortcut.duplicate".message()
            contentText = labels
            dialogStage?.let(::initOwner)
            dialogPane.stylesheets += stylesheetUrl
            dialogPane.styleClass  += "shortcut-error-alert"
            showAndWait()
        }
    }

    private fun GridPane.addGroupCell(node: Node, columnIndex: Int, rowIndex: Int) {
        add(node, columnIndex, rowIndex)
        when(columnIndex) {
            2 -> GridPane.setMargin(node, Insets(0.0, 0.0, 0.0, 8.0))
            else -> GridPane.setMargin(node, Insets.EMPTY)
        }
    }
}
