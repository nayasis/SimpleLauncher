package io.github.nayasis.simplelauncher.view

import io.github.nayasis.kotlin.basica.core.string.message
import io.github.nayasis.simplelauncher.service.ShortcutAction
import io.github.nayasis.simplelauncher.service.ShortcutGroup
import io.github.nayasis.simplelauncher.service.ShortcutSettings
import io.github.nayasis.simplelauncher.service.findDuplicateShortcuts
import io.github.nayasis.simplelauncher.service.toShortcutCombination
import javafx.event.ActionEvent
import javafx.geometry.Insets
import javafx.scene.control.Button
import javafx.scene.control.ButtonBar.ButtonData
import javafx.scene.control.ButtonType
import javafx.scene.control.Dialog
import javafx.scene.control.DialogPane
import javafx.scene.control.Label
import javafx.scene.control.ScrollPane
import javafx.scene.control.TextField
import javafx.scene.input.KeyEvent.KEY_PRESSED
import javafx.scene.layout.ColumnConstraints
import javafx.scene.layout.GridPane
import javafx.scene.layout.HBox
import javafx.scene.layout.Priority
import javafx.scene.layout.VBox
import javafx.stage.Stage
import javafx.geometry.Pos
import javafx.scene.text.Font
import javafx.scene.text.FontWeight
import javafx.util.Callback
import java.util.EnumMap

object ShortcutEditorDialog {

    fun show(owner: Stage?, shortcuts: ShortcutSettings): ShortcutSettings? {
        val bindings = shortcuts.toMutableBindings()
        val dialog = Dialog<ShortcutSettings>()
        val saveButtonType = ButtonType("btn.save".message(), ButtonData.OK_DONE)
        val cancelButtonType = ButtonType("btn.cancel".message(), ButtonData.CANCEL_CLOSE)
        val resetAllButtonType = ButtonType("btn.reset.all".message(), ButtonData.LEFT)

        dialog.title = "shortcut.dialog.title".message()
        owner?.let(dialog::initOwner)
        dialog.dialogPane = DialogPane().apply {
            buttonTypes.addAll(resetAllButtonType, cancelButtonType, saveButtonType)
            content = ScrollPane(buildContent(bindings)).apply {
                isFitToWidth = true
                prefViewportWidth = 760.0
                prefViewportHeight = 520.0
            }
        }

        (dialog.dialogPane.lookupButton(resetAllButtonType) as Button).addEventFilter(ActionEvent.ACTION) { event ->
            bindings.clear()
            ShortcutAction.entries.forEach { action ->
                bindings[action] = action.defaultCombination
            }
            dialog.dialogPane.content = ScrollPane(buildContent(bindings)).apply {
                isFitToWidth = true
                prefViewportWidth = 760.0
                prefViewportHeight = 520.0
            }
            event.consume()
        }

        (dialog.dialogPane.lookupButton(saveButtonType) as Button).addEventFilter(ActionEvent.ACTION) { event ->
            val duplicates = findDuplicateShortcuts(bindings)
            if (duplicates.isNotEmpty()) {
                val labels = duplicates.joinToString("\n") { group ->
                    group.joinToString(", ") { action -> action.messageKey.message() }
                }
                dialog.headerText = "msg.error.shortcut.duplicate".message().format(labels)
                event.consume()
            } else {
                dialog.headerText = null
            }
        }

        dialog.resultConverter = Callback { buttonType: ButtonType ->
            when (buttonType) {
                saveButtonType -> ShortcutSettings.fromBindings(bindings)
                else -> null
            }
        }

        return dialog.showAndWait().orElse(null)
    }

    private fun buildContent(bindings: EnumMap<ShortcutAction, javafx.scene.input.KeyCombination>): VBox {
        return VBox(8.0).apply {
            padding = Insets(8.0)
            children += buildHelpBox()
            ShortcutGroup.entries.forEach { group ->
                children += Label(group.messageKey.message()).apply {
                    font = Font.font(font.family, FontWeight.BOLD, 12.0)
                }
                children += buildGroup(group, bindings)
            }
        }
    }

    private fun buildHelpBox(): HBox {
        return HBox(8.0).apply {
            alignment = Pos.CENTER_LEFT
            padding = Insets(6.0, 8.0, 6.0, 8.0)
            style = "-fx-border-color: #a9bfd8; -fx-border-width: 1.2; -fx-border-radius: 6; -fx-background-radius: 6; -fx-background-color: #f7fafc;"
            children += Label("i").apply {
                alignment = Pos.CENTER
                minWidth = 18.0
                minHeight = 18.0
                prefWidth = 18.0
                prefHeight = 18.0
                font = Font.font(font.family, FontWeight.BOLD, 11.0)
                style = "-fx-text-fill: #2f6db3; -fx-background-color: white; -fx-border-color: #2f6db3; -fx-border-width: 1.4; -fx-border-radius: 9; -fx-background-radius: 9;"
            }
            children += Label("shortcut.dialog.help".message()).apply {
                isWrapText = true
                maxWidth = Double.MAX_VALUE
                HBox.setHgrow(this, Priority.ALWAYS)
            }
        }
    }

    private fun buildGroup(
        group: ShortcutGroup,
        bindings: EnumMap<ShortcutAction, javafx.scene.input.KeyCombination>,
    ): GridPane {
        return GridPane().apply {
            hgap = 6.0
            vgap = 8.0
            padding = Insets(0.0, 0.0, 6.0, 0.0)
            columnConstraints.addAll(
                ColumnConstraints().apply { prefWidth = 220.0 },
                ColumnConstraints().apply {
                    hgrow = Priority.ALWAYS
                    minWidth = 180.0
                },
                ColumnConstraints().apply { prefWidth = 120.0 },
                ColumnConstraints().apply { prefWidth = 90.0 },
            )

            add(Label("shortcut.column.action".message()), 0, 0)
            add(Label("shortcut.column.current".message()), 1, 0)
            add(Label("shortcut.column.default".message()), 2, 0)

            var row = 1
            ShortcutAction.entries
                .filter { it.group == group }
                .forEach { action ->
                    add(Label(action.messageKey.message()), 0, row)
                    add(createEditor(action, bindings), 1, row)
                    add(Label(action.defaultCombination.displayText), 2, row)
                    add(Button("btn.reset".message()).apply {
                        setOnAction {
                            bindings[action] = action.defaultCombination
                            (parent.lookup("#shortcut-${action.name}") as? TextField)?.text = action.defaultCombination.displayText
                        }
                    }, 3, row)
                    row++
                }
        }
    }

    private fun createEditor(
        action: ShortcutAction,
        bindings: EnumMap<ShortcutAction, javafx.scene.input.KeyCombination>,
    ): TextField {
        return TextField(bindings[action]?.displayText ?: action.defaultCombination.displayText).apply {
            id = "shortcut-${action.name}"
            isEditable = false
            isFocusTraversable = true
            addEventFilter(KEY_PRESSED) { event ->
                event.consume()
                event.toShortcutCombination()?.let { combination ->
                    bindings[action] = combination
                    text = combination.displayText
                }
            }
        }
    }
}
