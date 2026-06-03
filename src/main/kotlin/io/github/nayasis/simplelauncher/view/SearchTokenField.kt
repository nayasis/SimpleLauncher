package io.github.nayasis.simplelauncher.view

import io.github.nayasis.simplelauncher.service.SearchToken
import io.github.nayasis.simplelauncher.service.SearchTokenKind
import io.github.nayasis.simplelauncher.service.createOperatorSearchToken
import io.github.nayasis.simplelauncher.service.createTermSearchToken
import io.github.nayasis.simplelauncher.service.hasAdjacentSearchOperatorToken
import io.github.nayasis.simplelauncher.service.searchTokenLabel
import javafx.beans.property.StringProperty
import javafx.geometry.Pos
import javafx.scene.Node
import javafx.scene.control.Button
import javafx.scene.control.TextField
import javafx.scene.input.KeyCode
import javafx.scene.input.KeyEvent.KEY_PRESSED
import javafx.scene.layout.FlowPane
import kotlin.math.min

class SearchTokenField: FlowPane() {

    private data class UndoSnapshot(
        val tokens: List<SearchToken>,
        val cursorIndex: Int,
    )

    val input = TextField()

    private val tokens = ArrayList<SearchToken>()
    private val undoStack = ArrayList<UndoSnapshot>()
    private var cursorIndex = 0

    var onSearchChanged: (() -> Unit)? = null
    var onPlainEnter: (() -> Unit)? = null

    var text: String
        get() = input.text
        set(value) {
            input.text = value
        }

    init {
        hgap = 4.0
        vgap = 4.0
        alignment = Pos.CENTER_LEFT
        isPickOnBounds = true
        styleClass.add("token-field")
        styleClass.add("search-token-field")
        setOnMousePressed { event ->
            if ((event.target as? Node)?.styleClass?.contains("search-token-chip") == true) return@setOnMousePressed
            focusInput()
        }
        input.styleClass.add("token-field-input")
        input.styleClass.add("search-token-field-input")
        input.prefColumnCount = 12
        input.textProperty().addListener { _, _, _ -> fireSearchChanged() }
        input.addEventFilter(KEY_PRESSED) { event ->
            when {
                event.code == KeyCode.Z && event.isControlDown && !event.isShiftDown && text.isEmpty() -> {
                    restoreDeletedToken()
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
                event.code == KeyCode.ENTER -> {
                    if (tokens.isEmpty() || text.isBlank()) {
                        onPlainEnter?.invoke()
                    }
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

    fun focusInput() {
        input.requestFocus()
    }

    fun hasTokens(): Boolean = tokens.isNotEmpty()

    fun tokens(): List<SearchToken> = tokens.toList()

    private fun addTermTokenFromInput() {
        val token = createTermSearchToken(text) ?: return
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
        children += input
        tokens.drop(cursorIndex).forEachIndexed { offset, token -> addTokenButton(cursorIndex + offset, token) }
    }

    private fun addTokenButton(index: Int, token: SearchToken) {
        children += Button(searchTokenLabel(token)).apply {
            styleClass.add("token-chip")
            styleClass.add("search-token-chip")
            styleClass.add(token.kind.name.lowercase())
            isFocusTraversable = false
            setOnAction { removeTokenAt(index) }
        }
    }

    private fun fireSearchChanged() {
        onSearchChanged?.invoke()
    }

}
