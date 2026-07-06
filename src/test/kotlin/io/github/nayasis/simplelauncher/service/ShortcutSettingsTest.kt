package io.github.nayasis.simplelauncher.service

import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.shouldBe
import javafx.scene.input.KeyCode
import javafx.scene.input.KeyCodeCombination
import javafx.scene.input.KeyCombination
import org.junit.jupiter.api.Test

class ShortcutSettingsTest {

    @Test
    fun windowsDefaultsShouldUseControlShortcuts() {
        val settings = ShortcutSettings.load(emptyMap(), ShortcutPlatform.WINDOWS_LINUX)

        settings[ShortcutAction.SAVE_DETAIL] shouldBe KeyCodeCombination(KeyCode.S, KeyCombination.CONTROL_DOWN)
        settings[ShortcutAction.ADD_FILE] shouldBe KeyCodeCombination(KeyCode.N, KeyCombination.CONTROL_DOWN, KeyCombination.SHIFT_DOWN)
        settings[ShortcutAction.RUN_SELECTED_LINK] shouldBe KeyCodeCombination(KeyCode.ENTER)
    }

    @Test
    fun macDefaultsShouldUseMetaShortcuts() {
        val settings = ShortcutSettings.load(emptyMap(), ShortcutPlatform.MAC)

        settings[ShortcutAction.SAVE_DETAIL] shouldBe KeyCodeCombination(KeyCode.S, KeyCombination.META_DOWN)
        settings[ShortcutAction.ADD_FILE] shouldBe KeyCodeCombination(KeyCode.N, KeyCombination.META_DOWN, KeyCombination.SHIFT_DOWN)
        settings[ShortcutAction.RUN_SELECTED_LINK] shouldBe KeyCodeCombination(KeyCode.ENTER)
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
    fun blankConfiguredShortcutShouldFallbackToDefault() {
        val settings = ShortcutSettings.load(
            mapOf(ShortcutAction.SAVE_DETAIL.name to "   ")
        )

        settings.displayText(ShortcutAction.SAVE_DETAIL) shouldBe ShortcutAction.SAVE_DETAIL.defaultCombination(ShortcutPlatform.current()).displayText
    }

    @Test
    fun duplicateShortcutsShouldBeDetected() {
        val duplicate = KeyCombination.valueOf("Ctrl+S")

        val duplicates = findDuplicateShortcuts(
            mapOf(
                ShortcutAction.SAVE_DETAIL to duplicate,
                ShortcutAction.COPY_DETAIL to duplicate,
                ShortcutAction.SHOW_ABOUT to KeyCombination.valueOf("F1"),
            )
        )

        duplicates.single().shouldContainAll(ShortcutAction.SAVE_DETAIL, ShortcutAction.COPY_DETAIL)
    }

}
