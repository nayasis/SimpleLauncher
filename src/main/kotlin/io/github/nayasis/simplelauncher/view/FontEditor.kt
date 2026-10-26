package io.github.nayasis.simplelauncher.view

import io.github.nayasis.kotlin.basica.core.string.message
import io.github.nayasis.kotlin.basica.model.Messages
import io.github.nayasis.kotlin.swing.window.loadDefaultIcon
import io.github.nayasis.simplelauncher.service.Languages
import java.awt.BorderLayout
import java.awt.Dialog
import java.awt.Dimension
import java.awt.Font
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.awt.Insets
import java.awt.Window
import java.awt.event.KeyEvent
import java.text.ParseException
import javax.swing.BorderFactory
import javax.swing.JButton
import javax.swing.JComboBox
import javax.swing.JDialog
import javax.swing.JFormattedTextField
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JRootPane
import javax.swing.JScrollPane
import javax.swing.JSlider
import javax.swing.JSpinner
import javax.swing.KeyStroke
import javax.swing.SpinnerNumberModel
import javax.swing.UIManager
import javax.swing.text.DefaultFormatter

/** Preview all registered languages while editing; only Save changes the application font. */
class FontEditor(initial: Font = AppFonts.current()) {

    private val family = JComboBox((AppFonts.families + initial.family).distinct().sortedWith(String.CASE_INSENSITIVE_ORDER).toTypedArray()).apply {
        name = "fontFamily"
        selectedItem = initial.family
        maximumRowCount = 16
    }
    private val size = initial.size.coerceIn(AppFonts.MIN_SIZE, AppFonts.MAX_SIZE)
    private val slider = JSlider(AppFonts.MIN_SIZE, AppFonts.MAX_SIZE, size).apply {
        name = "fontSizeSlider"
        majorTickSpacing = 10
        minorTickSpacing = 1
        paintTicks = true
    }
    private val spinner = JSpinner(SpinnerNumberModel(size, AppFonts.MIN_SIZE, AppFonts.MAX_SIZE, 1)).apply {
        name = "fontSizeInput"
        editor = JSpinner.NumberEditor(this, "0").apply {
            textField.columns = 3
            textField.focusLostBehavior = JFormattedTextField.COMMIT
            (textField.formatter as DefaultFormatter).commitsOnValidEdit = true
        }
    }
    private val samples = Languages.available.associateWith { locale ->
        JLabel(Messages["font.preview.sample", locale]).apply { name = "fontPreview-${locale.toLanguageTag()}" }
    }
    private var dialog: JDialog? = null
    private var result: Font? = null
    private val saveButton = JButton("btn.save".message()).apply {
        name = "fontSave"
        addActionListener { save() }
    }

    init {
        family.addActionListener { refreshPreview() }
        slider.addChangeListener { spinner.value = slider.value }
        spinner.addChangeListener {
            slider.value = spinner.value as Int
            spinner.putClientProperty("JComponent.outline", null)
            refreshPreview()
        }
        refreshPreview()
    }

    fun showDialog(owner: Window?): Font? {
        result = null
        val window = JDialog(owner, "font.dialog.title".message(), Dialog.ModalityType.APPLICATION_MODAL).apply {
            name = "fontDialog"
            defaultCloseOperation = JDialog.DISPOSE_ON_CLOSE
            contentPane = buildContent()
            rootPane.defaultButton = saveButton
            rootPane.registerKeyboardAction({ dispose() }, KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JRootPane.WHEN_IN_FOCUSED_WINDOW)
            minimumSize = Dimension(520, 360)
            loadDefaultIcon()
            pack()
            // keep the footer accessible on a small screen or when the app already uses a large font
            graphicsConfiguration?.bounds?.let { screen ->
                setSize(width.coerceAtMost(screen.width - 80), height.coerceAtMost(screen.height - 80))
            }
            setLocationRelativeTo(owner)
        }
        dialog = window
        try {
            window.isVisible = true
        } finally {
            dialog = null
        }
        return result
    }

    internal fun buildContent(): JPanel = JPanel(BorderLayout(0, 12)).apply {
        border = BorderFactory.createEmptyBorder(16, 16, 12, 16)
        add(JPanel(GridBagLayout()).apply {
            fun constraints(x: Int, y: Int, weight: Double = 0.0) = GridBagConstraints().apply {
                gridx = x; gridy = y; weightx = weight
                fill = GridBagConstraints.HORIZONTAL
                anchor = GridBagConstraints.WEST
                insets = Insets(0, 0, 8, if (x == 0) 12 else 0)
            }
            add(JLabel("font.family".message()).apply { labelFor = family }, constraints(0, 0))
            add(family, constraints(1, 0, 1.0).apply { gridwidth = 2 })
            add(JLabel("font.size".message()).apply { labelFor = spinner }, constraints(0, 1))
            add(slider, constraints(1, 1, 1.0))
            add(spinner, constraints(2, 1).apply { insets = Insets(0, 8, 8, 0) })
        }, BorderLayout.NORTH)
        add(JPanel(BorderLayout(0, 8)).apply {
            add(JLabel("font.preview".message()), BorderLayout.NORTH)
            add(JScrollPane(JPanel(GridBagLayout()).apply {
                border = BorderFactory.createEmptyBorder(8, 12, 8, 12)
                samples.entries.forEachIndexed { index, (locale, sample) ->
                    add(JLabel(Languages.label(locale)).apply {
                        foreground = UIManager.getColor("Label.disabledForeground")
                    }, GridBagConstraints().apply {
                        gridx = 0; gridy = index * 2; weightx = 1.0
                        anchor = GridBagConstraints.WEST
                        insets = Insets(if (index == 0) 0 else 12, 0, 3, 0)
                    })
                    add(sample, GridBagConstraints().apply {
                        gridx = 0; gridy = index * 2 + 1; weightx = 1.0
                        anchor = GridBagConstraints.WEST
                    })
                }
                add(JPanel(), GridBagConstraints().apply { gridx = 0; gridy = samples.size * 2; weighty = 1.0 })
            }).apply {
                preferredSize = Dimension(660, 380)
                verticalScrollBar.unitIncrement = 20
                horizontalScrollBar.unitIncrement = 20
            }, BorderLayout.CENTER)
        }, BorderLayout.CENTER)
        add(JPanel(java.awt.FlowLayout(java.awt.FlowLayout.RIGHT, 8, 0)).apply {
            add(saveButton)
            add(JButton("btn.cancel".message()).apply {
                name = "fontCancel"
                addActionListener { dialog?.dispose() }
            })
        }, BorderLayout.SOUTH)
    }

    private fun selectedFont(): Font = AppFonts.font(family.selectedItem as String, spinner.value as Int)

    private fun save() {
        try {
            spinner.commitEdit()
            result = selectedFont()
            dialog?.dispose()
        } catch (_: ParseException) {
            spinner.putClientProperty("JComponent.outline", "error")
            (spinner.editor as JSpinner.NumberEditor).textField.requestFocusInWindow()
        }
    }

    private fun refreshPreview() {
        val font = selectedFont()
        samples.values.forEach { it.font = font }
    }
}
