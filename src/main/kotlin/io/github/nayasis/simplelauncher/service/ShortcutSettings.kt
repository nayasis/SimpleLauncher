package io.github.nayasis.simplelauncher.service

import javafx.scene.input.KeyCode
import javafx.scene.input.KeyCodeCombination
import javafx.scene.input.KeyCombination
import javafx.scene.input.KeyEvent
import java.util.EnumMap

enum class ShortcutGroup(val messageKey: String) {
    GENERAL("shortcut.group.general"),
    MENU("shortcut.group.menu"),
    TABLE("shortcut.group.table"),
}

enum class ShortcutPlatform {
    WINDOWS_LINUX,
    MAC,
    ;

    companion object {
        fun current(): ShortcutPlatform {
            val osName = System.getProperty("os.name").orEmpty().lowercase()
            return if (osName.contains("mac")) MAC else WINDOWS_LINUX
        }
    }
}

enum class ShortcutAction(
    val messageKey: String,
    val group: ShortcutGroup,
    private val defaultCombinationFactory: (ShortcutPlatform) -> KeyCombination,
) {
    SAVE_DETAIL("btn.save", ShortcutGroup.GENERAL, { commandShortcut(KeyCode.S, it) }),
    COPY_DETAIL("btn.copy", ShortcutGroup.GENERAL, { commandShortcut(KeyCode.D, it) }),
    CREATE_DETAIL("btn.new", ShortcutGroup.GENERAL, { commandShortcut(KeyCode.N, it) }),
    ADD_FILE("shortcut.action.addFile", ShortcutGroup.GENERAL, { commandShortcut(KeyCode.N, it, KeyCombination.SHIFT_DOWN) }),
    OPEN_FOLDER("btn.open.folder", ShortcutGroup.GENERAL, { commandShortcut(KeyCode.O, it) }),
    COPY_FOLDER("btn.copy.folder", ShortcutGroup.GENERAL, { commandShortcut(KeyCode.C, it, KeyCombination.SHIFT_DOWN) }),
    CHANGE_ICON("shortcut.action.changeIcon", ShortcutGroup.GENERAL, { commandShortcut(KeyCode.I, it) }),
    DELETE_DETAIL("shortcut.action.deleteDetail", ShortcutGroup.GENERAL, { KeyCodeCombination(KeyCode.DELETE, KeyCombination.SHIFT_DOWN) }),
    IMPORT_DATA("menu.file.import", ShortcutGroup.MENU, { commandShortcut(KeyCode.I, it, KeyCombination.SHIFT_DOWN) }),
    EXPORT_DATA("menu.file.export", ShortcutGroup.MENU, { commandShortcut(KeyCode.X, it, KeyCombination.SHIFT_DOWN) }),
    OPEN_SHORTCUT_SETTINGS("menu.settings.shortcut", ShortcutGroup.MENU, { commandShortcut(KeyCode.COMMA, it) }),
    TOGGLE_DESCRIPTION("menu.view.description", ShortcutGroup.MENU, { commandShortcut(KeyCode.E, it) }),
    TOGGLE_GROUP_FILTER("menu.view.showInputGroup", ShortcutGroup.MENU, { KeyCodeCombination(KeyCode.G, KeyCombination.ALT_DOWN) }),
    TOGGLE_ALWAYS_ON_TOP("menu.view.alwaysOnTop", ShortcutGroup.MENU, { commandShortcut(KeyCode.F, it, KeyCombination.SHIFT_DOWN) }),
    SHOW_ABOUT("menu.help.about", ShortcutGroup.MENU, { KeyCodeCombination(KeyCode.F1) }),
    RUN_SELECTED_LINK("shortcut.action.runSelectedLink", ShortcutGroup.TABLE, { KeyCodeCombination(KeyCode.ENTER) }),
    FOCUS_SEARCH("shortcut.action.focusSearch", ShortcutGroup.TABLE, { KeyCodeCombination(KeyCode.ESCAPE) }),
    DELETE_SELECTED_LINK("shortcut.action.deleteSelectedLink", ShortcutGroup.TABLE, { KeyCodeCombination(KeyCode.DELETE) }),
    MOVE_FOCUS_TO_DETAIL("shortcut.action.moveFocusToDetail", ShortcutGroup.TABLE, { KeyCodeCombination(KeyCode.TAB) }),
    COPY_FOLDER_FROM_TABLE("shortcut.action.copyFolderFromTable", ShortcutGroup.TABLE, { commandShortcut(KeyCode.C, it) }),
    OPEN_FOLDER_FROM_TABLE("shortcut.action.openFolderFromTable", ShortcutGroup.TABLE, { commandShortcut(KeyCode.O, it, KeyCombination.SHIFT_DOWN) }),
    ;

    val defaultCombination: KeyCombination
        get() = defaultCombination()

    fun defaultCombination(platform: ShortcutPlatform = ShortcutPlatform.current()): KeyCombination {
        return defaultCombinationFactory(platform)
    }
}

class ShortcutSettings private constructor(
    private val bindings: EnumMap<ShortcutAction, KeyCombination>,
) {

    operator fun get(action: ShortcutAction): KeyCombination {
        return bindings[action] ?: action.defaultCombination
    }

    fun displayText(action: ShortcutAction): String {
        return get(action).displayText
    }

    fun matches(action: ShortcutAction, event: KeyEvent): Boolean {
        return get(action).match(event)
    }

    fun toMutableBindings(): EnumMap<ShortcutAction, KeyCombination> {
        return EnumMap(bindings)
    }

    fun toConfigMap(): MutableMap<String, String> {
        return bindings.entries
            .associate { (action, combination) -> action.name to combination.toString() }
            .toMutableMap()
    }

    companion object {
        fun load(
            configured: Map<String, String>?,
            platform: ShortcutPlatform = ShortcutPlatform.current(),
        ): ShortcutSettings {
            val bindings = EnumMap<ShortcutAction, KeyCombination>(ShortcutAction::class.java)
            ShortcutAction.entries.forEach { action ->
                bindings[action] = configured?.get(action.name).toKeyCombinationOrNull() ?: action.defaultCombination(platform)
            }
            return ShortcutSettings(bindings)
        }

        fun fromBindings(bindings: Map<ShortcutAction, KeyCombination>): ShortcutSettings {
            val copied = EnumMap<ShortcutAction, KeyCombination>(ShortcutAction::class.java)
            ShortcutAction.entries.forEach { action ->
                copied[action] = bindings[action] ?: action.defaultCombination
            }
            return ShortcutSettings(copied)
        }
    }
}

private fun commandShortcut(
    keyCode: KeyCode,
    platform: ShortcutPlatform,
    vararg modifiers: KeyCombination.Modifier,
): KeyCombination {
    val primary = when (platform) {
        ShortcutPlatform.MAC -> KeyCombination.META_DOWN
        ShortcutPlatform.WINDOWS_LINUX -> KeyCombination.CONTROL_DOWN
    }
    return KeyCodeCombination(keyCode, primary, *modifiers)
}

fun findDuplicateShortcuts(bindings: Map<ShortcutAction, KeyCombination>): List<List<ShortcutAction>> {
    return bindings.entries
        .groupBy { it.value.toString() }
        .values
        .filter { it.size > 1 }
        .map { entries -> entries.map { it.key } }
}

fun KeyEvent.toShortcutCombination(): KeyCombination? {
    if (code in modifierKeys) return null
    val modifiers = buildList {
        if (isControlDown) add(KeyCombination.CONTROL_DOWN)
        if (isShiftDown) add(KeyCombination.SHIFT_DOWN)
        if (isAltDown) add(KeyCombination.ALT_DOWN)
        if (isMetaDown) add(KeyCombination.META_DOWN)
    }
    return KeyCodeCombination(code, *modifiers.toTypedArray())
}

private fun String?.toKeyCombinationOrNull(): KeyCombination? {
    if (this.isNullOrBlank()) return null
    return runCatching { KeyCombination.valueOf(this) }.getOrNull()
}

private val modifierKeys = setOf(
    KeyCode.SHIFT,
    KeyCode.CONTROL,
    KeyCode.ALT,
    KeyCode.META,
    KeyCode.WINDOWS,
    KeyCode.COMMAND,
)
