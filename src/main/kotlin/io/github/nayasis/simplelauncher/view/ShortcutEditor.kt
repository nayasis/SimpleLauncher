package io.github.nayasis.simplelauncher.view

import io.github.nayasis.kotlin.basica.core.string.message
import io.github.nayasis.kotlin.swing.misc.displayText
import io.github.nayasis.kotlin.swing.misc.isDarkLookAndFeel
import io.github.nayasis.kotlin.swing.misc.toKeyStroke
import io.github.nayasis.kotlin.swing.window.Dialog
import io.github.nayasis.kotlin.swing.window.loadDefaultIcon
import io.github.nayasis.simplelauncher.service.ShortcutAction
import io.github.nayasis.simplelauncher.service.ShortcutGroup
import io.github.nayasis.simplelauncher.service.ShortcutSettings
import io.github.nayasis.simplelauncher.service.findDuplicateShortcuts
import io.github.nayasis.simplelauncher.service.shortcutRestriction
import java.awt.BorderLayout
import java.awt.Color
import java.awt.Dimension
import java.awt.Font
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.awt.Insets
import java.awt.Window
import java.awt.event.FocusAdapter
import java.awt.event.FocusEvent
import java.awt.event.KeyAdapter
import java.awt.event.KeyEvent
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.util.EnumMap
import javax.swing.BorderFactory
import javax.swing.Box
import javax.swing.BoxLayout
import javax.swing.JButton
import javax.swing.JComponent
import javax.swing.JDialog
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JRootPane
import javax.swing.JScrollPane
import javax.swing.JTextArea
import javax.swing.JTextField
import javax.swing.KeyStroke
import javax.swing.UIManager

/**
 * dialog to rebind the shortcuts.
 *
 * a field records the next keys once it is clicked or Enter / Space is pressed on it. until then Tab moves on
 * and Escape closes the dialog, so the keyboard can always leave it.
 */
class ShortcutEditor {

    companion object {
        private val duplicateColors = listOf(
            Color(0xc62828),
            Color(0x1565c0),
            Color(0x2e7d32),
            Color(0x6a1b9a),
            Color(0xef6c00),
            Color(0x00838f),
            Color(0x5d4037),
        )
        private class HelpColors(val border: Color, val background: Color, val icon: Color)
        private val HELP_LIGHT = HelpColors(Color(0xa9bfd8), Color(0xf7fafc), Color(0x2f6db3))
        private val HELP_DARK  = HelpColors(Color(0x4a6178), Color(0x2b3440), Color(0x7aa7dc))
    }

    val title: String = "shortcut.dialog.title".message()

    private val bindings = EnumMap<ShortcutAction, KeyStroke?>(ShortcutAction::class.java)
    private val editors = EnumMap<ShortcutAction, JTextField>(ShortcutAction::class.java)
    /** the field waiting for keys, if any. */
    private var recording: ShortcutAction? = null
    private val notice = JLabel(" ").apply { foreground = Color(0xc62828) }

    private var result: ShortcutSettings? = null
    private var dialog: JDialog? = null

    /**
     * show the dialog modally.
     *
     * @return the edited shortcuts, or null when canceled
     */
    fun showDialog(owner: Window?, shortcuts: ShortcutSettings): ShortcutSettings? {
        result = null
        recording = null
        notice.text = " "
        editors.clear()
        bindings.clear()
        bindings.putAll(shortcuts.toMutableBindings())

        val content = buildContent()
        refreshDuplicateHighlighting()

        val dialog = JDialog(owner, title, java.awt.Dialog.ModalityType.APPLICATION_MODAL).apply {
            defaultCloseOperation = JDialog.DISPOSE_ON_CLOSE
            contentPane = JPanel(BorderLayout()).apply {
                border = BorderFactory.createEmptyBorder(4, 4, 8, 4)
                add(JScrollPane(content).apply {
                    border = BorderFactory.createEmptyBorder()
                    verticalScrollBar.unitIncrement = 16
                    preferredSize = Dimension(740, 540)
                }, BorderLayout.CENTER)
                add(buildFooter(), BorderLayout.SOUTH)
            }
            minimumSize = Dimension(600, 400)
            // reached only when no field is recording: a recording field consumes the key first
            rootPane.registerKeyboardAction({ dispose() }, KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JRootPane.WHEN_IN_FOCUSED_WINDOW)
            loadDefaultIcon()
            pack()
            setLocationRelativeTo(owner)
        }

        this.dialog = dialog
        dialog.isVisible = true
        this.dialog = null
        return result
    }

    private fun buildFooter(): JComponent {
        return JPanel().apply {
            layout = BoxLayout(this, BoxLayout.X_AXIS)
            border = BorderFactory.createEmptyBorder(8, 0, 0, 0)
            add(JButton("btn.reset.all".message()).apply {
                addActionListener { resetBindingsToDefault() }
            })
            add(Box.createHorizontalStrut(12))
            add(notice)
            add(Box.createHorizontalGlue())
            add(JButton("btn.save".message()).apply {
                addActionListener { save() }
            })
            add(Box.createHorizontalStrut(8))
            add(JButton("btn.cancel".message()).apply {
                addActionListener { dialog?.dispose() }
            })
        }
    }

    private fun save() {
        val duplicates = findDuplicateShortcuts(bindings)
        if(duplicates.isNotEmpty()) {
            showDuplicateAlert(duplicates)
        } else {
            result = ShortcutSettings.fromBindings(bindings)
            dialog?.dispose()
        }
    }

    internal fun buildContent(): JPanel {
        return JPanel().apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
            border = BorderFactory.createEmptyBorder(2, 2, 6, 2)
            add(buildHelpBox())
            ShortcutGroup.entries.forEach { group ->
                add(Box.createVerticalStrut(6))
                add(buildGroupTitle(group.messageKey.message()))
                add(Box.createVerticalStrut(4))
                add(buildGroup(group))
            }
        }.also { panel ->
            panel.components.filterIsInstance<JComponent>().forEach { it.alignmentX = 0f }
        }
    }

    private fun buildGroupTitle(text: String) = JLabel(text).apply {
        font = font.deriveFont(Font.BOLD, font.size2D + 3f)
    }

    private fun buildHelpBox(): JComponent {
        // the dialog is built each time it opens, so picking the colors here follows the current theme
        val colors = if (isDarkLookAndFeel()) HELP_DARK else HELP_LIGHT
        return JPanel(BorderLayout(8, 0)).apply {
            background = colors.background
            border = BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(colors.border),
                BorderFactory.createEmptyBorder(6, 8, 6, 8),
            )
            add(JLabel("ⓘ").apply {
                foreground = colors.icon
                font = font.deriveFont(Font.BOLD, font.size2D + 4f)
            }, BorderLayout.WEST)
            add(JTextArea("shortcut.dialog.help".message()).apply {
                isEditable = false
                isFocusable = false
                lineWrap = true
                wrapStyleWord = true
                isOpaque = false
                font = UIManager.getFont("Label.font")
            }, BorderLayout.CENTER)
            maximumSize = Dimension(Int.MAX_VALUE, preferredSize.height)
        }
    }

    private fun buildGroup(group: ShortcutGroup): JComponent {
        val panel = JPanel(GridBagLayout()).apply {
            border = BorderFactory.createEmptyBorder(0, 4, 6, 0)
        }
        fun cell(component: JComponent, column: Int, row: Int) {
            panel.add(component, GridBagConstraints().apply {
                gridx = column
                gridy = row
                anchor = GridBagConstraints.WEST
                fill = if(column == 1) GridBagConstraints.HORIZONTAL else GridBagConstraints.NONE
                weightx = if(column == 1) 1.0 else 0.0
                insets = Insets(4, if(column == 2) 8 else 0, 4, 4)
            })
        }

        cell(JLabel("shortcut.column.action".message()).apply { preferredSize = Dimension(162, preferredSize.height) }, 0, 0)
        cell(JLabel("shortcut.column.current".message()), 1, 0)
        cell(JLabel("shortcut.column.default".message()).apply { preferredSize = Dimension(98, preferredSize.height) }, 2, 0)

        var row = 1
        ShortcutAction.entries
            .filter { it.group == group }
            .forEach { action ->
                cell(JLabel(action.messageKey.message()), 0, row)
                cell(createEditor(action), 1, row)
                cell(JLabel(action.defaultKeyStroke.displayText), 2, row)
                cell(JPanel().apply {
                    layout = BoxLayout(this, BoxLayout.X_AXIS)
                    add(JButton("btn.reset".message()).apply {
                        addActionListener { bind(action, action.defaultKeyStroke) }
                    })
                    add(Box.createHorizontalStrut(4))
                    add(JButton("btn.clear".message()).apply {
                        addActionListener { bind(action, null) }
                    })
                }, 3, row)
                row++
            }
        panel.maximumSize = Dimension(Int.MAX_VALUE, panel.preferredSize.height)
        return panel
    }

    private fun createEditor(action: ShortcutAction): JTextField {
        return JTextField(bindings[action]?.displayText ?: "").apply {
            name = "shortcut-${action.name}"
            isEditable = false
            editors[action] = this
            addMouseListener(object: MouseAdapter() {
                override fun mousePressed(e: MouseEvent) {
                    requestFocusInWindow()
                    startRecording(action)
                }
            })
            addFocusListener(object: FocusAdapter() {
                override fun focusLost(e: FocusEvent) = stopRecording(action)
            })
            addKeyListener(object: KeyAdapter() {
                override fun keyPressed(e: KeyEvent) {
                    if(recording != action) {
                        if(e.modifiersEx == 0 && (e.keyCode == KeyEvent.VK_ENTER || e.keyCode == KeyEvent.VK_SPACE)) {
                            e.consume()
                            startRecording(action)
                        }
                        return
                    }
                    e.consume()
                    // a modifier alone is not a shortcut yet
                    val keyStroke = e.toKeyStroke() ?: return
                    stopRecording(action)
                    val restriction = shortcutRestriction(action, keyStroke)
                    if(restriction != null) {
                        notice.text = restriction.message().format(keyStroke.displayText)
                    } else {
                        bind(action, keyStroke)
                    }
                }
            })
        }
    }

    /** let the field of [action] record the next keys, Tab and Escape among them. */
    private fun startRecording(action: ShortcutAction) {
        if(recording == action) return
        recording?.let { stopRecording(it) }
        recording = action
        notice.text = " "
        editors[action]?.apply {
            focusTraversalKeysEnabled = false
            text = "shortcut.recording".message()
        }
    }

    private fun stopRecording(action: ShortcutAction) {
        if(recording != action) return
        recording = null
        editors[action]?.apply {
            focusTraversalKeysEnabled = true
            text = bindings[action]?.displayText ?: ""
        }
    }

    /** @param keyStroke the new shortcut of [action], null for none */
    private fun bind(action: ShortcutAction, keyStroke: KeyStroke?) {
        bindings[action] = keyStroke
        notice.text = " "
        editors[action]?.text = keyStroke?.displayText ?: ""
        refreshDuplicateHighlighting()
    }

    private fun resetBindingsToDefault() {
        bindings.clear()
        ShortcutAction.entries.forEach { action ->
            bindings[action] = action.defaultKeyStroke
        }
        editors.forEach { (action, editor) ->
            editor.text = bindings[action]?.displayText ?: ""
        }
        refreshDuplicateHighlighting()
    }

    private fun refreshDuplicateHighlighting() {
        val colorByAction = HashMap<ShortcutAction, Color>()
        findDuplicateShortcuts(bindings).forEachIndexed { index, actions ->
            actions.forEach { colorByAction[it] = duplicateColors[index % duplicateColors.size] }
        }
        editors.forEach { (action, editor) ->
            val color = colorByAction[action]
            // FlatLaf outline: a color draws the border in it
            editor.putClientProperty("JComponent.outline", color)
            editor.foreground = color ?: UIManager.getColor("TextField.foreground")
            editor.repaint()
        }
    }

    private fun showDuplicateAlert(duplicates: List<List<ShortcutAction>>) {
        val labels = duplicates.joinToString("\n") { group ->
            group.joinToString(", ") { action -> action.messageKey.message() }
        }
        Dialog.alert("msg.error.shortcut.duplicate".message(), labels)
    }

}
