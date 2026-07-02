package io.github.nayasis.simplelauncher.view

import io.github.nayasis.simplelauncher.model.normalizeHashtags
import javafx.scene.input.KeyCode
import javafx.scene.input.KeyEvent.KEY_PRESSED

class TokenField: TokenInputPane("plain-token-chip", 8) {

    private val tokens = LinkedHashSet<String>()
    private var suppressChange = false

    var onTokenChanged: (() -> Unit)? = null

    init {
        input.textProperty().addListener { _, _, _ ->
            if (!suppressChange) fireTokenChanged()
        }
        input.addEventFilter(KEY_PRESSED) { event ->
            when {
                event.code == KeyCode.ENTER -> {
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

    fun currentTokens(): HashSet<String> {
        return HashSet(tokens)
    }

    fun hasPendingInput(): Boolean {
        return input.text.isNotBlank()
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

    fun commitInput() {
        addFromInput()
    }

    private fun addFromInput(fireChanged: Boolean = true) {
        val added = normalizeHashtags(listOf(input.text))
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
            children += tokenButton(token) {
                tokens.remove(token)
                render()
                fireTokenChanged()
            }
        }
        addInput()
    }

    private fun fireTokenChanged() {
        onTokenChanged?.invoke()
    }

}
