package io.github.nayasis.simplelauncher.view

import io.github.nayasis.kotlin.swing.misc.Desktop
import io.github.nayasis.kotlin.swing.misc.getText
import io.github.nayasis.kotlin.swing.misc.runUiAndWait
import io.github.nayasis.kotlin.swing.misc.set
import io.github.nayasis.simplelauncher.service.SearchToken
import io.github.nayasis.simplelauncher.service.SearchTokenKind
import io.github.nayasis.simplelauncher.service.matchesSearchTokens
import io.github.nayasis.simplelauncher.service.searchTokenLabel
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.junit.jupiter.api.Test
import java.awt.event.InputEvent.CTRL_DOWN_MASK
import java.awt.event.InputEvent.SHIFT_DOWN_MASK
import java.awt.event.KeyEvent
import java.awt.event.MouseEvent

internal class SearchTokenFieldTest {

    @Test
    fun shiftEnterTurnsAndOrTextIntoOperatorTokens() = runUiAndWait<Unit> {
        val andField = SearchTokenField()
        val orField = SearchTokenField()

        andField.text = "AND"
        press(andField, KeyEvent.VK_ENTER, SHIFT_DOWN_MASK)

        andField.tokens() shouldBe listOf(SearchToken(SearchTokenKind.AND, "and"))

        orField.text = "OR"
        press(orField, KeyEvent.VK_ENTER, SHIFT_DOWN_MASK)

        orField.tokens() shouldBe listOf(SearchToken(SearchTokenKind.OR, "or"))
    }

    @Test
    fun notChipExcludesMatchingLinks() = runUiAndWait<Unit> {
        val field = SearchTokenField()

        field.text = "NOT"
        press(field, KeyEvent.VK_ENTER, SHIFT_DOWN_MASK)
        field.text = "pp"
        press(field, KeyEvent.VK_ENTER, SHIFT_DOWN_MASK)

        field.tokens() shouldBe listOf(SearchToken(SearchTokenKind.NOT, "not"), SearchToken(SearchTokenKind.TERM, "pp"))
        val titles = listOf("Bandi Namer", "KeepAlive", "Markpad", "MongoDB Compass 1.24.1", "ppInk")
        titles.filter { matchesSearchTokens(listOf(it), field.tokens()) } shouldBe titles - "ppInk"
    }

    @Test
    fun chipChangesNotifyButTypingDoesNot() = runUiAndWait<Unit> {
        val field = SearchTokenField()
        var changes = 0
        field.onTokensChanged = { changes++ }

        field.text = "pp"
        changes shouldBe 0

        press(field, KeyEvent.VK_ENTER, SHIFT_DOWN_MASK)
        changes shouldBe 1

        press(field, KeyEvent.VK_BACK_SPACE)
        changes shouldBe 2
        field.tokens() shouldBe emptyList()
    }

    @Test
    fun pendingTextNextToChipsBecomesOneTermChip() = runUiAndWait<Unit> {
        val field = SearchTokenField()
        var notified = 0
        field.onTokensChanged = { notified++ }

        // no chips: plain text keeps its own syntax
        field.text = "pp ink"
        field.commitPendingTextNextToChips()
        field.tokens() shouldBe emptyList()
        field.text shouldBe "pp ink"

        field.text = "NOT"
        press(field, KeyEvent.VK_ENTER, SHIFT_DOWN_MASK)
        notified = 0
        field.text = "pp ink"
        field.commitPendingTextNextToChips()

        field.tokens() shouldBe listOf(SearchToken(SearchTokenKind.NOT, "not"), SearchToken(SearchTokenKind.TERM, "pp ink"))
        field.text shouldBe ""
        notified shouldBe 0
    }

    private fun click(target: java.awt.Component, button: Int = MouseEvent.BUTTON1, count: Int = 1) {
        val event = MouseEvent(target, MouseEvent.MOUSE_CLICKED, System.currentTimeMillis(), 0, 1, 1, count, false, button)
        target.mouseListeners.forEach { it.mouseClicked(event) }
    }

    private fun fieldWith(vararg terms: String) = SearchTokenField().apply {
        terms.forEach { text = it; press(this, KeyEvent.VK_ENTER, CTRL_DOWN_MASK) }
    }

    /** run [block] with the clipboard, putting back what was there. */
    private fun withClipboard(block: () -> Unit) {
        val saved = runCatching { Desktop.clipboard.getText() }.getOrNull()
        try { block() } finally { saved?.let { Desktop.clipboard.set(it) } }
    }

    @Test
    fun clickSelectsTheChipAndPutsTheCursorInFrontOfIt() = runUiAndWait<Unit> {
        val field = fieldWith("alpha", "beta", "gamma")
        field.text = "typed"

        click(field.chips()[1])

        field.chips().map { it.selected } shouldBe listOf(false, true, false)
        field.searchState().cursorIndex shouldBe 1
        field.text shouldBe "typed"
        field.input.caretPosition shouldBe "typed".length
        field.tokens().size shouldBe 3

        // typing drops the selection
        field.text = "typed!"
        field.chips().none { it.selected } shouldBe true
    }

    @Test
    fun rightClickRemovesTheChip() = runUiAndWait<Unit> {
        val field = fieldWith("alpha", "beta")
        var changes = 0
        field.onTokensChanged = { changes++ }

        click(field.chips()[0], MouseEvent.BUTTON3)

        field.tokens() shouldBe listOf(SearchToken(SearchTokenKind.TERM, "beta"))
        changes shouldBe 1
    }

    @Test
    fun cursorBetweenChipsTakesHalfTheOldGap() = runUiAndWait<Unit> {
        val field = fieldWith("alpha", "beta")
        click(field.chips()[1])   // cursor between the two chips
        field.setSize(600, field.preferredSize.height)
        field.doLayout()

        val (left, right) = field.chips()
        val gap = right.x - (left.x + left.width)
        val metrics = field.input.getFontMetrics(field.input.font)
        val oldGap = metrics.charWidth('m') / 2 + 2 * 2 + 3 * 2
        (gap <= oldGap / 2 + 2) shouldBe true
        (field.input.width >= 1) shouldBe true

        // typing brings the padding back
        field.text = "x"
        field.input.insets.left shouldBe 2
    }

    @Test
    fun shiftEnterOnATermJoinsItWithOr() = runUiAndWait<Unit> {
        val field = SearchTokenField()
        var searches = 0
        field.onTokensChanged = { searches++ }

        field.text = "alpha"
        press(field, KeyEvent.VK_ENTER, SHIFT_DOWN_MASK)          // start: no OR
        field.text = "beta"
        press(field, KeyEvent.VK_ENTER, SHIFT_DOWN_MASK)          // after a term: OR
        field.text = "AND"
        press(field, KeyEvent.VK_ENTER, SHIFT_DOWN_MASK)
        field.text = "gamma"
        press(field, KeyEvent.VK_ENTER, SHIFT_DOWN_MASK)          // AND becomes OR

        field.tokens().map { searchTokenLabel(it) } shouldBe listOf("alpha", "OR", "beta", "OR", "gamma")
        field.text shouldBe ""
        searches shouldBe 4

        // Ctrl+Enter still adds a plain term
        field.text = "delta"
        press(field, KeyEvent.VK_ENTER, CTRL_DOWN_MASK)
        field.tokens().map { searchTokenLabel(it) }.last() shouldBe "delta"
        field.tokens().size shouldBe 6
    }

    @Test
    fun rightClickLeavesTheCursorWhereTheChipWas() = runUiAndWait<Unit> {
        val field = fieldWith("alpha", "beta", "gamma")
        field.text = "typed"
        field.searchState().cursorIndex shouldBe 3

        click(field.chips()[1], MouseEvent.BUTTON3)

        field.tokens().map { it.value } shouldBe listOf("alpha", "gamma")
        field.searchState().cursorIndex shouldBe 1
        field.text shouldBe "typed"
        field.input.caretPosition shouldBe 0

        // keyboard removal keeps the cursor where it is
        field.text = ""
        press(field, KeyEvent.VK_DELETE)
        field.tokens().map { it.value } shouldBe listOf("alpha")
        field.searchState().cursorIndex shouldBe 1
    }

    @Test
    fun doubleClickCopiesTheChip() = runUiAndWait<Unit> {
        withClipboard {
            val field = fieldWith("alpha", "beta")
            click(field.chips()[1], count = 2)
            Desktop.clipboard.getText() shouldBe "beta"
            field.tokens().size shouldBe 2
        }
    }

    @Test
    fun blankRightClickPastesTextOrAChip() = runUiAndWait<Unit> {
        withClipboard {
            Desktop.clipboard.set("pasted\nvalue")

            // no chip after the cursor: goes into the text
            val field = fieldWith("alpha")
            field.text = "x"
            field.input.caretPosition = 1
            click(field.input, MouseEvent.BUTTON3)
            field.text shouldBe "xpasted value"
            field.tokens().size shouldBe 1

            // a chip after the cursor: becomes a chip at the cursor, typed text kept
            val middle = fieldWith("alpha", "beta")
            click(middle.chips()[1])   // cursor between alpha and beta
            middle.text = "kept"
            click(middle, MouseEvent.BUTTON3)
            middle.tokens().map { it.value } shouldBe listOf("alpha", "pasted value", "beta")
            middle.text shouldBe "kept"
            middle.searchState().cursorIndex shouldBe 2
        }
    }

    @Test
    fun controlEnterTurnsAndOrTextIntoTermTokens() = runUiAndWait<Unit> {
        val andField = SearchTokenField()
        val orField = SearchTokenField()

        andField.text = "AND"
        press(andField, KeyEvent.VK_ENTER, CTRL_DOWN_MASK)

        andField.tokens() shouldBe listOf(SearchToken(SearchTokenKind.TERM, "AND"))

        orField.text = "OR"
        press(orField, KeyEvent.VK_ENTER, CTRL_DOWN_MASK)

        orField.tokens() shouldBe listOf(SearchToken(SearchTokenKind.TERM, "OR"))
    }

    @Test
    fun operatorAndOrTokenChipsUseCompactOperatorStyle() = runUiAndWait<Unit> {
        val field = SearchTokenField()

        field.text = "AND"
        press(field, KeyEvent.VK_ENTER, SHIFT_DOWN_MASK)

        field.chips().first().style shouldBe chipStyleOf(SearchTokenKind.AND)
    }

    @Test
    fun literalAndOrTermTokenChipsKeepNormalTokenStyle() = runUiAndWait<Unit> {
        val field = SearchTokenField()

        field.text = "AND"
        press(field, KeyEvent.VK_ENTER, CTRL_DOWN_MASK)

        field.chips().first().style shouldBe chipStyleOf(SearchTokenKind.TERM)
        chipStyleOf(SearchTokenKind.TERM) shouldNotBe chipStyleOf(SearchTokenKind.AND)
    }

    @Test
    fun plainEnterSubmitsWithoutAddingTokens() = runUiAndWait<Unit> {
        val field = SearchTokenField()
        var submitted = 0
        field.onPlainEnter = { submitted++ }

        field.text = "keyword"
        press(field, KeyEvent.VK_ENTER)

        submitted shouldBe 1
        field.tokens() shouldBe emptyList()
    }

    @Test
    fun backspaceRemovesTokenAndControlZRestoresIt() = runUiAndWait<Unit> {
        val field = SearchTokenField()
        field.text = "alpha"
        press(field, KeyEvent.VK_ENTER, CTRL_DOWN_MASK)

        press(field, KeyEvent.VK_BACK_SPACE)
        field.tokens() shouldBe emptyList()

        press(field, KeyEvent.VK_Z, CTRL_DOWN_MASK)
        field.tokens() shouldBe listOf(SearchToken(SearchTokenKind.TERM, "alpha"))
    }

    @Test
    fun tokensKeepTheInputAttached() = runUiAndWait<Unit> {
        val field = SearchTokenField()
        field.text = "alpha"
        press(field, KeyEvent.VK_ENTER, CTRL_DOWN_MASK)

        field.input.parent shouldBe field
        field.components.last() shouldBe field.input
    }

    // call the listeners directly: dispatchEvent goes through the focus manager, which a test has no use for
    private fun press(field: SearchTokenField, code: Int, modifiers: Int = 0) {
        val event = KeyEvent(field.input, KeyEvent.KEY_PRESSED, System.currentTimeMillis(), modifiers, code, KeyEvent.CHAR_UNDEFINED)
        field.input.keyListeners.forEach { it.keyPressed(event) }
    }

}
