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
import javafx.scene.control.ScrollPane
import javafx.scene.control.TextField
import javafx.scene.input.KeyCombination
import javafx.scene.input.KeyEvent.KEY_PRESSED
import javafx.scene.layout.ColumnConstraints
import javafx.scene.layout.GridPane
import javafx.scene.layout.Priority
import javafx.scene.layout.VBox
import javafx.stage.Modality
import javafx.stage.Stage
import tornadofx.View
import tornadofx.action
import tornadofx.borderpane
import tornadofx.button
import tornadofx.gridpane
import tornadofx.hbox
import tornadofx.hgrow
import tornadofx.label
import tornadofx.region
import tornadofx.scrollpane
import tornadofx.textfield
import tornadofx.vbox
import java.util.EnumMap

class ShortcutEditor: View("shortcut.dialog.title".message()) {

    companion object {
        private const val STYLESHEET_PATH = "/view/main/shortcut-dialog.css"
        private const val ERROR_ALERT_STYLE_CLASS = "shortcut-error-alert"

        private val duplicateStyleClasses = listOf(
            "shortcut-duplicate-1",
            "shortcut-duplicate-2",
            "shortcut-duplicate-3",
            "shortcut-duplicate-4",
            "shortcut-duplicate-5",
            "shortcut-duplicate-6",
            "shortcut-duplicate-7",
        )
    }

    private val stylesheetUrl = ShortcutEditor::class.java.getResource(STYLESHEET_PATH)!!.toExternalForm()
    private val bindings = EnumMap<ShortcutAction, KeyCombination>(ShortcutAction::class.java)
    private val editors = EnumMap<ShortcutAction, TextField>(ShortcutAction::class.java)

    private var result: ShortcutSettings? = null
    private var dialogStage: Stage? = null

    private val contentScroll: ScrollPane = scrollpane {
        styleClass += "shortcut-dialog-scroll"
        isFitToWidth = true
        prefViewportWidth = 730.0
        prefViewportHeight = 520.0
        padding = Insets.EMPTY
    }

    private val resetAllButton = button("btn.reset.all".message()) {
        action { resetBindingsToDefault() }
    }

    private val saveButton = button("btn.save".message()).apply {
        addEventFilter(ActionEvent.ACTION) { event ->
            val duplicates = findDuplicateShortcuts(bindings)
            if(duplicates.isNotEmpty()) {
                showDuplicateAlert(duplicates)
                event.consume()
            } else {
                result = ShortcutSettings.fromBindings(bindings)
                dialogStage?.close()
            }
        }
    }

    private val cancelButton = button("btn.cancel".message()) {
        action { dialogStage?.close() }
    }

    override val root = borderpane {
        styleClass += "shortcut-dialog-root"
        stylesheets += stylesheetUrl
        center = contentScroll
        bottom = hbox(8.0) {
            styleClass += "shortcut-dialog-footer"
            add(resetAllButton)
            region {
                hgrow = Priority.ALWAYS
            }
            add(saveButton)
            add(cancelButton)
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
        return vbox {
            styleClass += "shortcut-dialog-content"
            add(buildHelpBox())
            ShortcutGroup.entries.forEach { group ->
                add(buildGroupTitle(group.messageKey.message()))
                add(buildGroup(group))
            }
        }
    }

    private fun buildGroupTitle(text: String) = label(text) {
        styleClass += "shortcut-group-title"
    }

    private fun buildHelpBox() = hbox {
        styleClass += "shortcut-help-box"
        label("i") {
            styleClass += "shortcut-help-icon"
        }
        label("shortcut.dialog.help".message()) {
            styleClass += "shortcut-help-text"
            isWrapText = true
            maxWidth = Double.MAX_VALUE
            hgrow = Priority.ALWAYS
        }
    }

    private fun buildGroup(group: ShortcutGroup): GridPane {
        return gridpane {
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

            addGroupCell(label("shortcut.column.action".message()), 0, 0)
            addGroupCell(label("shortcut.column.current".message()), 1, 0)
            addGroupCell(label("shortcut.column.default".message()), 2, 0)

            var row = 1
            ShortcutAction.entries
                .filter { it.group == group }
                .forEach { action ->
                    addGroupCell(label(action.messageKey.message()), 0, row)
                    addGroupCell(createEditor(action), 1, row)
                    addGroupCell(label(action.defaultCombination.displayText), 2, row)
                    addGroupCell(button("btn.reset".message()) {
                        action {
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
        return textfield(bindings[action]?.displayText ?: action.defaultCombination.displayText) {
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

    private fun resetBindingsToDefault() {
        bindings.clear()
        ShortcutAction.entries.forEach { action ->
            bindings[action] = action.defaultCombination
        }
        editors.forEach { (action, editor) ->
            editor.text = bindings[action]!!.displayText
        }
        refreshDuplicateHighlighting()
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
            headerText = "msg.error.shortcut.duplicate".message()
            contentText = labels
            dialogStage?.let(::initOwner)
            dialogPane.stylesheets += stylesheetUrl
            dialogPane.styleClass += ERROR_ALERT_STYLE_CLASS
            showAndWait()
        }
    }

    private fun GridPane.addGroupCell(node: Node, columnIndex: Int, rowIndex: Int) {
        if(node.parent === this) {
            GridPane.setConstraints(node, columnIndex, rowIndex)
        } else {
            add(node, columnIndex, rowIndex)
        }
        when(columnIndex) {
            2 -> GridPane.setMargin(node, Insets(0.0, 0.0, 0.0, 8.0))
            else -> GridPane.setMargin(node, Insets.EMPTY)
        }
    }
}
