package io.github.nayasis.simplelauncher.view

import io.github.nayasis.simplelauncher.model.normalizeHashtags
import javafx.scene.control.Button
import javafx.scene.control.TextField
import javafx.geometry.Pos
import javafx.scene.Node
import javafx.scene.input.KeyCode
import javafx.scene.input.KeyEvent.KEY_PRESSED
import javafx.scene.layout.FlowPane

private val TOKEN_DELIMITER = "[,\\s]+".toRegex()

class TokenField: FlowPane() {

    private val tokens = LinkedHashSet<String>()
    private val input = TextField()
    private var suppressChange = false

    var onTokenChanged: (() -> Unit)? = null

    init {
        hgap = 4.0
        vgap = 4.0
        alignment = Pos.CENTER_LEFT
        isFocusTraversable = false
        isPickOnBounds = true
        styleClass.add("token-field")
        setOnMousePressed { event ->
            if ((event.target as? Node)?.styleClass?.contains("token-chip") == true) return@setOnMousePressed
            input.requestFocus()
        }
        input.styleClass.add("token-field-input")
        input.prefColumnCount = 8
        input.textProperty().addListener { _, _, _ ->
            if (!suppressChange) fireTokenChanged()
        }
        input.addEventFilter(KEY_PRESSED) { event ->
            when {
                event.code == KeyCode.ENTER || event.code == KeyCode.SPACE || event.code == KeyCode.COMMA -> {
                    addFromInput()
                    event.consume()
                }
                event.code == KeyCode.BACK_SPACE && input.text.isEmpty() && tokens.isNotEmpty() -> {
                    tokens.remove(tokens.last())
                    render()
                    fireTokenChanged()
                    event.consume()
                }
            }
        }
        render()
    }

    fun setTokens(values: Iterable<String?>?) {
        suppressChange = true
        try {
            tokens.clear()
            tokens.addAll(normalizeHashtags(values))
            input.clear()
            render()
        } finally {
            suppressChange = false
        }
    }

    fun getTokens(): HashSet<String> {
        addFromInput(fireChanged = false)
        return HashSet(tokens)
    }

    fun clearTokens() {
        suppressChange = true
        try {
            tokens.clear()
            input.clear()
            render()
        } finally {
            suppressChange = false
        }
    }

    private fun addFromInput(fireChanged: Boolean = true) {
        val added = normalizeHashtags(input.text.split(TOKEN_DELIMITER))
            .filter { tokens.add(it) }
            .isNotEmpty()
        input.clear()
        if (added) {
            render()
            if (fireChanged) fireTokenChanged()
        }
    }

    private fun render() {
        children.clear()
        tokens.forEach { token ->
            children += Button(token).apply {
                styleClass.add("token-chip")
                isFocusTraversable = false
                setOnAction {
                    tokens.remove(token)
                    render()
                    fireTokenChanged()
                }
            }
        }
        children += input
    }

    private fun fireTokenChanged() {
        onTokenChanged?.invoke()
    }

}
