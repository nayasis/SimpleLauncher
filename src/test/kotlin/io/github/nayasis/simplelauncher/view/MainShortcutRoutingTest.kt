package io.github.nayasis.simplelauncher.view

import io.github.nayasis.simplelauncher.service.ShortcutAction
import io.github.nayasis.simplelauncher.service.ShortcutSettings
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import org.junit.jupiter.api.Test
import java.awt.Canvas
import java.awt.event.InputEvent.CTRL_DOWN_MASK
import java.awt.event.KeyEvent

internal class MainShortcutRoutingTest {

    private fun press(code: Int, modifiers: Int = 0) =
        KeyEvent(Canvas(), KeyEvent.KEY_PRESSED, 0, modifiers, code, KeyEvent.CHAR_UNDEFINED)

    @Test
    fun menuShortcutShouldInvokeMatchingHandler() {
        val shortcuts = ShortcutSettings.load(
            mapOf(ShortcutAction.OPEN_SHORTCUT_SETTINGS.name to "Ctrl+Comma")
        )
        val event = press(KeyEvent.VK_COMMA, CTRL_DOWN_MASK)
        var invoked = false

        val handled = handleMainShortcutEvent(
            shortcuts = shortcuts,
            event = event,
            tableShortcutContext = false,
            handlers = MainShortcutHandlers(
                onOpenShortcutSettings = { invoked = true }
            ),
        )

        handled.shouldBeTrue()
        invoked.shouldBeTrue()
        event.isConsumed.shouldBeTrue()
    }

    @Test
    fun tableShortcutShouldRequireTableContext() {
        val shortcuts = ShortcutSettings.load(
            mapOf(ShortcutAction.RUN_SELECTED_LINK.name to "Enter")
        )
        val event = press(KeyEvent.VK_ENTER)
        var invoked = false

        val handled = handleMainShortcutEvent(
            shortcuts = shortcuts,
            event = event,
            tableShortcutContext = false,
            handlers = MainShortcutHandlers(
                onRunSelectedLink = { invoked = true }
            ),
        )

        handled.shouldBeFalse()
        invoked.shouldBeFalse()
        event.isConsumed.shouldBeFalse()
    }

    @Test
    fun generalShortcutShouldInvokeMatchingHandler() {
        val shortcuts = ShortcutSettings.load(
            mapOf(ShortcutAction.SAVE_DETAIL.name to "Ctrl+S")
        )
        val event = press(KeyEvent.VK_S, CTRL_DOWN_MASK)
        var invoked = false

        val handled = handleMainShortcutEvent(
            shortcuts = shortcuts,
            event = event,
            tableShortcutContext = false,
            handlers = MainShortcutHandlers(
                onSaveDetail = { invoked = true }
            ),
        )

        handled.shouldBeTrue()
        invoked.shouldBeTrue()
        event.isConsumed.shouldBeTrue()
    }

}
