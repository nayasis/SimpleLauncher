package io.github.nayasis.simplelauncher.view

import javafx.css.PseudoClass
import javafx.geometry.Pos
import javafx.scene.Node
import javafx.scene.control.Button
import javafx.scene.control.OverrunStyle
import javafx.scene.control.TextField
import javafx.scene.layout.HBox
import javafx.scene.layout.Region
import javafx.scene.shape.Rectangle

private val FOCUSED = PseudoClass.getPseudoClass("focused")
private val INPUT_FOCUSED = PseudoClass.getPseudoClass("input-focused")

abstract class TokenInputPane(
    private val chipClass: String,
    inputColumns: Int,
): HBox() {

    val input = TextField()

    init {
        spacing = 4.0
        alignment = Pos.CENTER_LEFT
        isFocusTraversable = false
        isPickOnBounds = true
        clip = Rectangle().apply {
            widthProperty().bind(this@TokenInputPane.widthProperty())
            heightProperty().bind(this@TokenInputPane.heightProperty())
        }
        styleClass.add("token-field")
        setOnMousePressed { event ->
            if ((event.target as? Node)?.styleClass?.contains(chipClass) == true) return@setOnMousePressed
            focusInput()
        }
        input.styleClass.add("token-field-input")
        input.style = """
            -fx-background-color: transparent;
            -fx-border-color: transparent;
            -fx-border-width: 0;
            -fx-background-insets: 0;
            -fx-padding: 0 2px;
            -fx-focus-color: transparent;
            -fx-faint-focus-color: transparent;
        """.trimIndent()
        input.prefColumnCount = inputColumns
        input.focusedProperty().addListener { _, _, focused ->
            pseudoClassStateChanged(FOCUSED, focused)
            pseudoClassStateChanged(INPUT_FOCUSED, focused)
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
            textOverrun = OverrunStyle.CLIP
            minWidth = Region.USE_PREF_SIZE
            setOnAction { onAction() }
        }
    }

}
