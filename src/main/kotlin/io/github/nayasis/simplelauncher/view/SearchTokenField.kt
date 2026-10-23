package io.github.nayasis.simplelauncher.view

import io.github.nayasis.kotlin.swing.control.token.ChipStyle
import io.github.nayasis.kotlin.swing.control.token.TokenInputPane
import io.github.nayasis.kotlin.swing.misc.Desktop
import io.github.nayasis.kotlin.swing.misc.getText
import io.github.nayasis.kotlin.swing.misc.set
import io.github.nayasis.simplelauncher.service.SearchToken
import io.github.nayasis.simplelauncher.service.SearchTokenKind
import io.github.nayasis.simplelauncher.service.createCommandOrTermSearchToken
import io.github.nayasis.simplelauncher.service.createLiteralTermSearchToken
import io.github.nayasis.simplelauncher.service.createOperatorSearchToken
import io.github.nayasis.simplelauncher.service.canInsertSearchToken
import io.github.nayasis.simplelauncher.service.insertTermWithOr
import io.github.nayasis.simplelauncher.service.searchTokenLabel
import java.awt.Color
import java.awt.Dimension
import java.awt.event.InputEvent.CTRL_DOWN_MASK
import java.awt.event.InputEvent.SHIFT_DOWN_MASK
import java.awt.event.KeyAdapter
import java.awt.event.KeyEvent
import javax.swing.BorderFactory
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener
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
// horizontal gap TokenInputPane leaves between its children, and the side padding it gives the input
private const val CHIP_GAP = 3
private const val INPUT_PADDING = 2
private const val CARET_WIDTH = 1

private val TERM_CHIP     = ChipStyle.DEFAULT
private val OPERATOR_CHIP = ChipStyle(Color(0xfbe9e9), Color(0xe3b5b5), Color(0x8a5050), fontScale = 0.65f,
    dark = ChipStyle(Color(0x402a2a), Color(0x7a4646), Color(0xdba3a3)))
private val PAREN_CHIP    = ChipStyle(Color(0xf2eef8), Color(0xc9b7e6), Color(0x746483), fontScale = 0.8f,
    dark = ChipStyle(Color(0x352f41), Color(0x5e5079), Color(0xbcaee0)))

/** chip style of a token kind. AND / OR / NOT operators share a compact style, unlike terms spelled AND / OR / NOT. */
internal fun chipStyleOf(kind: SearchTokenKind): ChipStyle = when (kind) {
    SearchTokenKind.TERM  -> TERM_CHIP
    SearchTokenKind.AND,
    SearchTokenKind.OR,
    SearchTokenKind.NOT   -> OPERATOR_CHIP
    SearchTokenKind.PAREN -> PAREN_CHIP
}

/**
 * search input of terms and operators, shown as chips around a text cursor.
 *
 * - Enter submits ([onPlainEnter]); Escape calls [onEscape]
 * - Ctrl+Enter adds the text as a term, Shift+Enter as an operator when it is AND / OR / NOT, Ctrl+Shift+Enter as an operator
 * - Left / Right at the ends of the text move the input between chips
 * - Backspace / Delete in an empty input remove the selected chip, else the chip next to it
 * - Ctrl+Z in an empty input undoes the last change of the chips
 * - click on a chip selects it and puts the cursor in front of it; double click copies it; right click removes it
 * - right click elsewhere pastes the clipboard: as text when no chip follows the cursor, as a new chip otherwise
 */
class SearchTokenField: TokenInputPane(SEARCH_INPUT_COLUMNS) {

    private data class UndoSnapshot(
        val tokens: List<SearchToken>,
        val cursorIndex: Int,
    )

    private val tokens = ArrayList<SearchToken>()
    private val undoStack = ArrayList<UndoSnapshot>()
    private var cursorIndex = 0
    private var suppressSearchChange = false
    /** index of the clicked chip, -1 for none. any other change of the chips or the cursor drops it. */
    private var selectedChip = -1

    var onSearchChanged: (() -> Unit)? = null
    /** called when the user adds, removes or restores a chip; plain typing only fires [onSearchChanged]. */
    var onTokensChanged: (() -> Unit)? = null
    var onPlainEnter: (() -> Unit)? = null
    var onEscape: (() -> Unit)? = null

    var text: String
        get() = input.text
        set(value) {
            input.text = value
        }

    init {
        input.document.addDocumentListener(object: DocumentListener {
            override fun insertUpdate(e: DocumentEvent) = onTextChanged()
            override fun removeUpdate(e: DocumentEvent) = onTextChanged()
            override fun changedUpdate(e: DocumentEvent) {}
        })
        input.addKeyListener(object: KeyAdapter() {
            override fun keyPressed(e: KeyEvent) {
                if (e.isConsumed) return
                if (handleKey(e)) e.consume()
            }
        })
        render()
    }

    private fun handleKey(e: KeyEvent): Boolean {
        // Alt combinations belong to the search history
        if (e.isAltDown) return false
        val modifiers = e.modifiersEx and (CTRL_DOWN_MASK or SHIFT_DOWN_MASK)
        when {
            e.keyCode == KeyEvent.VK_Z && modifiers == CTRL_DOWN_MASK && text.isEmpty() -> undoTokenChange()
            e.keyCode == KeyEvent.VK_ESCAPE -> onEscape?.invoke()
            e.keyCode == KeyEvent.VK_ENTER && modifiers == (CTRL_DOWN_MASK or SHIFT_DOWN_MASK) -> addOperatorTokenFromInput()
            e.keyCode == KeyEvent.VK_ENTER && modifiers == CTRL_DOWN_MASK -> addTermTokenFromInput()
            e.keyCode == KeyEvent.VK_ENTER && modifiers == SHIFT_DOWN_MASK -> addCommandOrTermTokenFromInput()
            e.keyCode == KeyEvent.VK_ENTER -> onPlainEnter?.invoke()
            // a selected chip is removed rather than the one next to the cursor
            (e.keyCode == KeyEvent.VK_BACK_SPACE || e.keyCode == KeyEvent.VK_DELETE) && text.isEmpty() && selectedChip in tokens.indices -> removeTokenAt(selectedChip)
            // with Shift or Ctrl the arrows select or jump within the text
            e.keyCode == KeyEvent.VK_LEFT && modifiers == 0 && input.selectedText == null && input.caretPosition == 0 && cursorIndex > 0 -> {
                cursorIndex--
                render()
            }
            e.keyCode == KeyEvent.VK_RIGHT && modifiers == 0 && input.selectedText == null && input.caretPosition == text.length && cursorIndex < tokens.size -> {
                cursorIndex++
                render()
            }
            e.keyCode == KeyEvent.VK_DELETE && text.isEmpty() && cursorIndex < tokens.size -> removeTokenAt(cursorIndex)
            e.keyCode == KeyEvent.VK_BACK_SPACE && text.isEmpty() && cursorIndex > 0 -> removeTokenAt(cursorIndex - 1)
            else -> return false
        }
        return true
    }

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
        if (token.kind == SearchTokenKind.TERM) {
            // Shift+Enter on a term means "or this": one edit, so the search runs once on the complete expression
            commitEdit { cursorIndex = insertTermWithOr(tokens, cursorIndex, token) }
            return
        }
        if (!canInsertSearchToken(tokens, cursorIndex, token)) return
        insertToken(token)
    }

    private fun addOperatorTokenFromInput() {
        val token = createOperatorSearchToken(text.trim()) ?: return
        if (!canInsertSearchToken(tokens, cursorIndex, token)) return
        insertToken(token)
    }

    /**
     * turn all of the typed text into one term chip.
     * does not call [onTokensChanged]: the caller is about to search anyway.
     */
    fun commitPendingText() {
        val token = createLiteralTermSearchToken(text) ?: return
        insertToken(token, notifyTokensChanged = false)
    }

    /** what to search with: the chips, or while there are none, the typed text as one term. */
    fun searchTokens(): List<SearchToken> =
        tokens.toList().ifEmpty { listOfNotNull(createLiteralTermSearchToken(text)) }

    private fun insertToken(token: SearchToken, notifyTokensChanged: Boolean = true) =
        commitEdit(notifyTokensChanged) {
            tokens.add(cursorIndex, token)
            cursorIndex++
        }

    /** change the chips with [edit], which consumes the typed text, then redraw and notify once. */
    private fun commitEdit(notifyTokensChanged: Boolean = true, edit: () -> Unit) {
        rememberForUndo()
        edit()
        suppressSearchChange = true
        try {
            input.text = ""
        } finally {
            suppressSearchChange = false
        }
        render()
        fireSearchChanged()
        if (notifyTokensChanged) onTokensChanged?.invoke()
    }

    /** @param moveCursor put the cursor where the chip was, instead of keeping it where it is */
    private fun removeTokenAt(index: Int, moveCursor: Boolean = false) {
        if (index !in tokens.indices) return
        rememberForUndo()
        tokens.removeAt(index)
        cursorIndex = when {
            moveCursor          -> index
            index < cursorIndex -> cursorIndex - 1
            else                -> cursorIndex
        }.coerceAtMost(tokens.size)
        render()
        if (moveCursor) {
            focusInput()
            input.caretPosition = 0
        }
        fireSearchChanged()
        onTokensChanged?.invoke()
    }

    private fun rememberForUndo() {
        undoStack += UndoSnapshot(tokens.toList(), cursorIndex)
    }

    private fun undoTokenChange() {
        val snapshot = undoStack.removeLastOrNull() ?: return
        tokens.clear()
        tokens.addAll(snapshot.tokens)
        cursorIndex = min(snapshot.cursorIndex, tokens.size)
        input.text = ""
        render()
        fireSearchChanged()
        onTokensChanged?.invoke()
    }

    private fun render(keepSelection: Boolean = false) {
        if (!keepSelection) selectedChip = -1
        updateInputColumns()
        render(
            before = tokens.take(cursorIndex).mapIndexed { index, token -> tokenChip(index, token) },
            after  = tokens.drop(cursorIndex).mapIndexed { offset, token -> tokenChip(cursorIndex + offset, token) },
        )
    }

    private fun tokenChip(index: Int, token: SearchToken) =
        chip(
            label         = searchTokenLabel(token),
            style         = chipStyleOf(token.kind),
            // another program can hold the system clipboard for a moment; a missed copy beats an error dialog
            onDoubleClick = { runCatching { Desktop.clipboard.set(searchTokenLabel(token)) } },
            onRightClick  = { removeTokenAt(index, moveCursor = true) },
        ) { selectChip(index) }.apply { selected = index == selectedChip }

    private fun selectChip(index: Int) {
        if (index !in tokens.indices) return
        selectedChip = index
        cursorIndex  = index
        render(keepSelection = true)
        focusInput()
        // typed text now sits in front of the chip: put the caret against it
        input.caretPosition = text.length
    }

    override fun onBlankRightClick() {
        val pasted = runCatching { Desktop.clipboard.getText() }.getOrDefault("")
            .replace(Regex("\\s*\\R\\s*"), " ").trim()
        if (pasted.isEmpty()) return
        if (cursorIndex < tokens.size) {
            // text between chips would be ignored by the search, so it goes in as a chip; typed text stays
            val token = createLiteralTermSearchToken(pasted) ?: return
            rememberForUndo()
            tokens.add(cursorIndex, token)
            cursorIndex++
            render()
            fireSearchChanged()
            onTokensChanged?.invoke()
        } else {
            focusInput()
            input.replaceSelection(pasted)
        }
    }

    private fun onTextChanged() {
        updateInputColumns()
        if (selectedChip >= 0) {
            selectedChip = -1
            chips().forEach { it.selected = false }
        }
        if(!suppressSearchChange) fireSearchChanged()
    }

    private fun fireSearchChanged() {
        onSearchChanged?.invoke()
    }

    private fun updateInputColumns() {
        // an empty input between chips only marks the cursor, so it shrinks to little more than the caret
        val cursorOnly = tokens.isNotEmpty() && text.isEmpty()
        // JTextField sizes itself by columns whenever they are set, ignoring a preferred size: 0 lets it apply
        val columns = when {
            tokens.isEmpty() -> SEARCH_INPUT_COLUMNS
            cursorOnly       -> 0
            else             -> max(text.length + 1, 1)
        }
        if (input.columns != columns) {
            input.columns = columns
            revalidate()
        }
        val padding = if (cursorOnly) 0 else INPUT_PADDING
        if (input.insets.left != padding) input.border = BorderFactory.createEmptyBorder(0, padding, 0, padding)
        val preferredSize = if (cursorOnly) cursorOnlySize() else null
        if (input.isPreferredSizeSet || preferredSize != null) {
            input.preferredSize = preferredSize
            revalidate()
        }
    }

    // the visible gap is the input plus a chip gap on each side: half of what half a padded character used to take
    private fun cursorOnlySize(): Dimension {
        input.preferredSize = null
        val metrics = input.getFontMetrics(input.font)
        val gaps    = CHIP_GAP * 2
        val before  = metrics.charWidth('m') / 2 + INPUT_PADDING * 2 + gaps
        val width   = (before / 2 - gaps).coerceAtLeast(CARET_WIDTH + 1)
        return Dimension(width, input.preferredSize.height)
    }

}
