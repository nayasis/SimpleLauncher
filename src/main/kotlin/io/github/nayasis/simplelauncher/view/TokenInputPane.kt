package io.github.nayasis.simplelauncher.view

import javafx.css.PseudoClass
import javafx.geometry.Pos
import javafx.scene.Node
import javafx.scene.control.Button
import javafx.scene.control.TextField
import javafx.scene.layout.FlowPane

private val FOCUSED = PseudoClass.getPseudoClass("focused")

abstract class TokenInputPane(
    private val chipClass: String,
    inputColumns: Int,
): FlowPane() {

    val input = TextField()

    init {
        hgap = 4.0
        vgap = 4.0
        alignment = Pos.CENTER_LEFT
        isFocusTraversable = false
        isPickOnBounds = true
        styleClass.add("token-field")
        setOnMousePressed { event ->
            if ((event.target as? Node)?.styleClass?.contains(chipClass) == true) return@setOnMousePressed
            focusInput()
        }
        input.styleClass.add("token-field-input")
        input.prefColumnCount = inputColumns
        input.focusedProperty().addListener { _, _, focused ->
            pseudoClassStateChanged(FOCUSED, focused)
        }
    }

    fun focusInput() {
        input.requestFocus()
    }

    protected fun addInput() {
        children += input
    }

    protected fun tokenButton(
        label: String,
        vararg styleClasses: String,
        onAction: () -> Unit,
    ): Button {
        return Button(label).apply {
            styleClass.add("token-chip")
            styleClass.add(chipClass)
            styleClass.addAll(styleClasses)
            isFocusTraversable = false
            setOnAction { onAction() }
        }
    }

}
