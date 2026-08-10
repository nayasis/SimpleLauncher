package io.github.nayasis.simplelauncher.view

import io.github.nayasis.simplelauncher.service.ShortcutAction
import io.github.nayasis.simplelauncher.service.ShortcutSettings
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.shouldBe
import javafx.scene.input.KeyCode
import javafx.scene.input.KeyEvent
import org.junit.jupiter.api.Test

internal class MainShortcutRoutingTest {

    @Test
    fun menuShortcutShouldInvokeMatchingHandler() {
        val shortcuts = ShortcutSettings.load(
            mapOf(ShortcutAction.OPEN_SHORTCUT_SETTINGS.name to "Ctrl+Comma")
        )
        val event = KeyEvent(KeyEvent.KEY_PRESSED, "", "", KeyCode.COMMA, false, true, false, false)
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
        val event = KeyEvent(KeyEvent.KEY_PRESSED, "", "", KeyCode.ENTER, false, false, false, false)
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
        val event = KeyEvent(KeyEvent.KEY_PRESSED, "", "", KeyCode.S, false, true, false, false)
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
