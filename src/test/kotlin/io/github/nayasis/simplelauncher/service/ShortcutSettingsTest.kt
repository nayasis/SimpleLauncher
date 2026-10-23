package io.github.nayasis.simplelauncher.service

import io.github.nayasis.kotlin.swing.misc.displayText
import io.github.nayasis.kotlin.swing.misc.keyStroke
import io.github.nayasis.kotlin.swing.misc.toKeyStroke
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import java.awt.event.InputEvent.ALT_DOWN_MASK
import java.awt.event.InputEvent.CTRL_DOWN_MASK
import java.awt.event.InputEvent.META_DOWN_MASK
import java.awt.event.InputEvent.SHIFT_DOWN_MASK
import java.awt.event.KeyEvent

class ShortcutSettingsTest {

    @Test
    fun windowsDefaultsShouldUseControlShortcuts() {
        val settings = ShortcutSettings.load(emptyMap(), ShortcutPlatform.WINDOWS_LINUX)

        settings[ShortcutAction.SAVE_DETAIL] shouldBe keyStroke(KeyEvent.VK_S, CTRL_DOWN_MASK)
        settings[ShortcutAction.ADD_FILE] shouldBe keyStroke(KeyEvent.VK_N, CTRL_DOWN_MASK or SHIFT_DOWN_MASK)
        settings[ShortcutAction.OPEN_SHORTCUT_SETTINGS] shouldBe keyStroke(KeyEvent.VK_COMMA, CTRL_DOWN_MASK)
        settings[ShortcutAction.RUN_SELECTED_LINK] shouldBe keyStroke(KeyEvent.VK_ENTER)
    }

    @Test
    fun macDefaultsShouldUseMetaShortcuts() {
        val settings = ShortcutSettings.load(emptyMap(), ShortcutPlatform.MAC)

        settings[ShortcutAction.SAVE_DETAIL] shouldBe keyStroke(KeyEvent.VK_S, META_DOWN_MASK)
        settings[ShortcutAction.ADD_FILE] shouldBe keyStroke(KeyEvent.VK_N, META_DOWN_MASK or SHIFT_DOWN_MASK)
        settings[ShortcutAction.OPEN_SHORTCUT_SETTINGS] shouldBe keyStroke(KeyEvent.VK_COMMA, META_DOWN_MASK)
        settings[ShortcutAction.RUN_SELECTED_LINK] shouldBe keyStroke(KeyEvent.VK_ENTER)
    }

    @Test
    fun configuredShortcutShouldOverrideDefault() {
        val settings = ShortcutSettings.load(
            mapOf(
                ShortcutAction.SAVE_DETAIL.name to "Alt+S",
                ShortcutAction.SHOW_ABOUT.name to "Ctrl+F1",
            )
        )

        settings.displayText(ShortcutAction.SAVE_DETAIL) shouldBe "Alt+S"
        settings.displayText(ShortcutAction.SHOW_ABOUT) shouldBe "Ctrl+F1"
    }

    @Test
    fun shortcutsSavedByTheJavaFxVersionShouldStillLoad() {
        val settings = ShortcutSettings.load(
            mapOf(
                ShortcutAction.DELETE_DETAIL.name to "Shift+Delete",
                ShortcutAction.FOCUS_SEARCH.name to "Esc",
                ShortcutAction.OPEN_SHORTCUT_SETTINGS.name to "Ctrl+Comma",
            ),
            ShortcutPlatform.WINDOWS_LINUX,
        )

        settings[ShortcutAction.DELETE_DETAIL] shouldBe keyStroke(KeyEvent.VK_DELETE, SHIFT_DOWN_MASK)
        settings[ShortcutAction.FOCUS_SEARCH] shouldBe keyStroke(KeyEvent.VK_ESCAPE)
        settings[ShortcutAction.OPEN_SHORTCUT_SETTINGS] shouldBe keyStroke(KeyEvent.VK_COMMA, CTRL_DOWN_MASK)
    }

    @Test
    fun savedShortcutsShouldLoadBackUnchanged() {
        val settings = ShortcutSettings.load(emptyMap(), ShortcutPlatform.WINDOWS_LINUX)

        val reloaded = ShortcutSettings.load(settings.toConfigMap(), ShortcutPlatform.MAC)

        ShortcutAction.entries.forEach { reloaded[it] shouldBe settings[it] }
    }

    @Test
    fun blankConfiguredShortcutShouldFallbackToDefault() {
        val settings = ShortcutSettings.load(
            mapOf(ShortcutAction.SAVE_DETAIL.name to "   ")
        )

        settings.displayText(ShortcutAction.SAVE_DETAIL) shouldBe ShortcutAction.SAVE_DETAIL.defaultKeyStroke(ShortcutPlatform.current()).displayText
    }

    @Test
    fun unboundShortcutShouldStayUnboundAfterReload() {
        val settings = ShortcutSettings.fromBindings(mapOf(ShortcutAction.SAVE_DETAIL to null))

        val reloaded = ShortcutSettings.load(settings.toConfigMap())

        reloaded[ShortcutAction.SAVE_DETAIL].shouldBeNull()
        reloaded.displayText(ShortcutAction.SAVE_DETAIL) shouldBe ""
        reloaded[ShortcutAction.COPY_DETAIL] shouldBe ShortcutAction.COPY_DETAIL.defaultKeyStroke
    }

    @Test
    fun unboundShortcutsAreNoDuplicates() {
        findDuplicateShortcuts(mapOf(ShortcutAction.SAVE_DETAIL to null, ShortcutAction.COPY_DETAIL to null)) shouldBe emptyList()
    }

    @Test
    fun defaultShortcutsShouldPassTheRestrictions() {
        ShortcutPlatform.entries.forEach { platform ->
            ShortcutAction.entries.forEach { action ->
                shortcutRestriction(action, action.defaultKeyStroke(platform)) shouldBe null
            }
        }
    }

    @Test
    fun globalShortcutsShouldLeaveTextInputKeysAlone() {
        // typing
        shortcutRestriction(ShortcutAction.SAVE_DETAIL, keyStroke(KeyEvent.VK_A)).shouldNotBeNull()
        shortcutRestriction(ShortcutAction.SAVE_DETAIL, keyStroke(KeyEvent.VK_ESCAPE)).shouldNotBeNull()
        // cut in a text field
        shortcutRestriction(ShortcutAction.DELETE_DETAIL, keyStroke(KeyEvent.VK_DELETE, SHIFT_DOWN_MASK)).shouldNotBeNull()
        // editing and the search history
        shortcutRestriction(ShortcutAction.SAVE_DETAIL, keyStroke(KeyEvent.VK_Z, CTRL_DOWN_MASK)).shouldNotBeNull()
        shortcutRestriction(ShortcutAction.SAVE_DETAIL, keyStroke(KeyEvent.VK_C, META_DOWN_MASK)).shouldNotBeNull()
        shortcutRestriction(ShortcutAction.SAVE_DETAIL, keyStroke(KeyEvent.VK_DOWN, ALT_DOWN_MASK)).shouldNotBeNull()

        shortcutRestriction(ShortcutAction.SAVE_DETAIL, keyStroke(KeyEvent.VK_F5)).shouldBeNull()
        shortcutRestriction(ShortcutAction.SAVE_DETAIL, keyStroke(KeyEvent.VK_S, ALT_DOWN_MASK)).shouldBeNull()
    }

    @Test
    fun tableShortcutsTakeAnyKey() {
        shortcutRestriction(ShortcutAction.FOCUS_SEARCH, keyStroke(KeyEvent.VK_ESCAPE)).shouldBeNull()
        shortcutRestriction(ShortcutAction.MOVE_FOCUS_TO_DETAIL, keyStroke(KeyEvent.VK_TAB)).shouldBeNull()
    }

    @Test
    fun duplicateShortcutsShouldBeDetected() {
        val duplicate = "Ctrl+S".toKeyStroke()!!

        val duplicates = findDuplicateShortcuts(
            mapOf(
                ShortcutAction.SAVE_DETAIL to duplicate,
                ShortcutAction.COPY_DETAIL to keyStroke(KeyEvent.VK_S, CTRL_DOWN_MASK),
                ShortcutAction.SHOW_ABOUT to "F1".toKeyStroke()!!,
            )
        )

        duplicates.single().shouldContainAll(ShortcutAction.SAVE_DETAIL, ShortcutAction.COPY_DETAIL)
    }

}
