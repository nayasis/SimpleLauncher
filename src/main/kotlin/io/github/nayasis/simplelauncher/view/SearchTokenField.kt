package io.github.nayasis.simplelauncher.view

import io.github.nayasis.simplelauncher.service.SearchToken
import io.github.nayasis.simplelauncher.service.SearchTokenKind
import io.github.nayasis.simplelauncher.service.createCommandOrTermSearchToken
import io.github.nayasis.simplelauncher.service.createLiteralTermSearchToken
import io.github.nayasis.simplelauncher.service.createOperatorSearchToken
import io.github.nayasis.simplelauncher.service.hasAdjacentSearchOperatorToken
import io.github.nayasis.simplelauncher.service.searchTokenLabel
import javafx.beans.property.StringProperty
import javafx.scene.input.KeyCode
import javafx.scene.input.KeyEvent.KEY_PRESSED
import kotlin.math.max
import kotlin.math.min

data class SearchFieldState(
    val tokens: List<SearchToken> = emptyList(),
    val text: String? = null,
    val cursorIndex: Int = 0,
) {

    fun isBlank(): Boolean {
        return tokens.isEmpty() && text.isNullOrBlank()
    }

    fun displayText(): String {
        val index = cursorIndex.coerceIn(0, tokens.size)
        val parts = ArrayList<String>()
        tokens.take(index).forEach { parts += searchTokenLabel(it) }
        text?.trim()?.takeIf { it.isNotEmpty() }?.let { parts += it }
        tokens.drop(index).forEach { parts += searchTokenLabel(it) }
        return parts.joinToString(" ").trim()
    }

}

private const val SEARCH_INPUT_COLUMNS = 12

class SearchTokenField: TokenInputPane("search-token-chip", SEARCH_INPUT_COLUMNS) {

    private data class UndoSnapshot(
        val tokens: List<SearchToken>,
        val cursorIndex: Int,
    )

    private val tokens = ArrayList<SearchToken>()
    private val undoStack = ArrayList<UndoSnapshot>()
    private var cursorIndex = 0
    private var suppressSearchChange = false

    var onSearchChanged: (() -> Unit)? = null
    var onPlainEnter: (() -> Unit)? = null
    var onEscape: (() -> Unit)? = null

    var text: String
        get() = input.text
        set(value) {
            input.text = value
        }

    init {
        styleClass.add("search-token-field")
        input.styleClass.add("search-token-field-input")
        input.textProperty().addListener { _, _, _ ->
            updateInputColumns()
            if(!suppressSearchChange) fireSearchChanged()
        }
        input.addEventFilter(KEY_PRESSED) { event ->
            when {
                event.code == KeyCode.Z && event.isControlDown && !event.isShiftDown && text.isEmpty() -> {
                    restoreDeletedToken()
                    event.consume()
                }
                event.code == KeyCode.ESCAPE -> {
                    onEscape?.invoke()
                    event.consume()
                }
                event.code == KeyCode.ENTER && event.isControlDown && event.isShiftDown -> {
                    addOperatorTokenFromInput()
                    event.consume()
                }
                event.code == KeyCode.ENTER && event.isControlDown -> {
                    addTermTokenFromInput()
                    event.consume()
                }
                event.code == KeyCode.ENTER && event.isShiftDown -> {
                    addCommandOrTermTokenFromInput()
                    event.consume()
                }
                event.code == KeyCode.ENTER -> {
                    onPlainEnter?.invoke()
                    event.consume()
                }
                event.code == KeyCode.LEFT && input.caretPosition == 0 && cursorIndex > 0 -> {
                    cursorIndex--
                    render()
                    event.consume()
                }
                event.code == KeyCode.RIGHT && input.caretPosition == input.text.length && cursorIndex < tokens.size -> {
                    cursorIndex++
                    render()
                    event.consume()
                }
                event.code == KeyCode.DELETE && text.isEmpty() && cursorIndex < tokens.size -> {
                    removeTokenAt(cursorIndex)
                    event.consume()
                }
                event.code == KeyCode.BACK_SPACE && text.isEmpty() && cursorIndex > 0 -> {
                    removeTokenAt(cursorIndex - 1)
                    event.consume()
                }
            }
        }
        render()
    }

    fun textProperty(): StringProperty = input.textProperty()

    fun hasTokens(): Boolean = tokens.isNotEmpty()

    fun tokens(): List<SearchToken> = tokens.toList()

    fun currentTermValues(): HashSet<String> {
        return tokens
            .filter { it.kind == SearchTokenKind.TERM }
            .mapTo(HashSet()) { it.value }
    }

    fun searchText(): String {
        return searchState().displayText()
    }

    fun setSearchText(value: String?) {
        setSearchState(SearchFieldState(text = value?.trim()))
    }

    fun searchState(): SearchFieldState {
        return SearchFieldState(
            tokens = tokens.toList(),
            text = text.trim().takeIf { it.isNotEmpty() },
            cursorIndex = cursorIndex,
        )
    }

    fun setSearchState(state: SearchFieldState?) {
        suppressSearchChange = true
        try {
            tokens.clear()
            tokens.addAll(state?.tokens ?: emptyList())
            undoStack.clear()
            cursorIndex = (state?.cursorIndex ?: tokens.size).coerceIn(0, tokens.size)
            input.text = state?.text?.trim() ?: ""
            render()
        } finally {
            suppressSearchChange = false
        }
        fireSearchChanged()
    }

    fun clearTokens() {
        setSearchText(null)
    }

    private fun addTermTokenFromInput() {
        val token = createLiteralTermSearchToken(text) ?: return
        insertToken(token)
    }

    private fun addCommandOrTermTokenFromInput() {
        val token = createCommandOrTermSearchToken(text) ?: return
        if (token.kind in listOf(SearchTokenKind.AND, SearchTokenKind.OR) && hasAdjacentSearchOperatorToken(tokens, cursorIndex)) return
        insertToken(token)
    }

    private fun addOperatorTokenFromInput() {
        val token = createOperatorSearchToken(text.trim()) ?: return
        if (token.kind in listOf(SearchTokenKind.AND, SearchTokenKind.OR) && hasAdjacentSearchOperatorToken(tokens, cursorIndex)) return
        insertToken(token)
    }

    private fun insertToken(token: SearchToken) {
        tokens.add(cursorIndex, token)
        cursorIndex++
        input.clear()
        render()
        fireSearchChanged()
    }

    private fun removeTokenAt(index: Int) {
        if (index !in tokens.indices) return
        undoStack += UndoSnapshot(tokens.toList(), cursorIndex)
        tokens.removeAt(index)
        cursorIndex = min(if (index < cursorIndex) cursorIndex - 1 else cursorIndex, tokens.size)
        render()
        fireSearchChanged()
    }

    private fun restoreDeletedToken() {
        val snapshot = undoStack.removeLastOrNull() ?: return
        tokens.clear()
        tokens.addAll(snapshot.tokens)
        cursorIndex = min(snapshot.cursorIndex, tokens.size)
        input.clear()
        render()
        fireSearchChanged()
    }

    private fun render() {
        children.clear()
        tokens.take(cursorIndex).forEachIndexed { index, token -> addTokenButton(index, token) }
        updateInputColumns()
        addInput()
        tokens.drop(cursorIndex).forEachIndexed { offset, token -> addTokenButton(cursorIndex + offset, token) }
    }

    private fun addTokenButton(index: Int, token: SearchToken) {
        val label = searchTokenLabel(token)
        val styleClasses = mutableListOf(token.kind.name.lowercase())
        if (token.kind in listOf(SearchTokenKind.AND, SearchTokenKind.OR)) {
            styleClasses += "operator-word"
        }
        children += tokenButton(label, *styleClasses.toTypedArray()) { removeTokenAt(index) }
    }

    private fun fireSearchChanged() {
        onSearchChanged?.invoke()
    }

    private fun updateInputColumns() {
        input.prefColumnCount = if (tokens.isEmpty()) {
            SEARCH_INPUT_COLUMNS
        } else {
            max(text.length + 1, 1)
        }
    }

}
