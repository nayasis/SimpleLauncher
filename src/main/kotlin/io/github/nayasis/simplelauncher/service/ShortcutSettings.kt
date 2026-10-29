package io.github.nayasis.simplelauncher.service

import io.github.nayasis.kotlin.swing.misc.displayText
import io.github.nayasis.kotlin.swing.misc.keyStroke
import io.github.nayasis.kotlin.swing.misc.matches
import io.github.nayasis.kotlin.swing.misc.modifiersEx
import io.github.nayasis.kotlin.swing.misc.toKeyStroke
import java.awt.event.InputEvent.ALT_DOWN_MASK
import java.awt.event.InputEvent.CTRL_DOWN_MASK
import java.awt.event.InputEvent.META_DOWN_MASK
import java.awt.event.InputEvent.SHIFT_DOWN_MASK
import java.awt.event.KeyEvent
import java.awt.event.KeyEvent.*
import java.util.EnumMap
import javax.swing.KeyStroke

enum class ShortcutGroup(val messageKey: String) {
    GENERAL("shortcut.group.general"),
    MENU("shortcut.group.menu"),
    TABLE("shortcut.group.table"),
}

/** [commandMask]: the primary modifier of the platform, Ctrl or Command. */
enum class ShortcutPlatform(val commandMask: Int) {
    WINDOWS_LINUX(CTRL_DOWN_MASK),
    MAC(META_DOWN_MASK),
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
    private val defaultKeyStrokeFactory: (ShortcutPlatform) -> KeyStroke,
) {
    SAVE_DETAIL("btn.save", ShortcutGroup.GENERAL, { commandShortcut(VK_S, it) }),
    COPY_DETAIL("btn.copy", ShortcutGroup.GENERAL, { commandShortcut(VK_D, it) }),
    CREATE_DETAIL("btn.new", ShortcutGroup.GENERAL, { commandShortcut(VK_N, it) }),
    ADD_FILE("shortcut.action.addFile", ShortcutGroup.GENERAL, { commandShortcut(VK_N, it, SHIFT_DOWN_MASK) }),
    OPEN_FOLDER("btn.open.folder", ShortcutGroup.GENERAL, { commandShortcut(VK_O, it) }),
    COPY_FOLDER("btn.copy.folder", ShortcutGroup.GENERAL, { commandShortcut(VK_C, it, SHIFT_DOWN_MASK) }),
    CHANGE_ICON("shortcut.action.changeIcon", ShortcutGroup.GENERAL, { commandShortcut(VK_I, it) }),
    // not Shift+Delete: text fields cut with it
    DELETE_DETAIL("shortcut.action.deleteDetail", ShortcutGroup.GENERAL, { commandShortcut(VK_DELETE, it, SHIFT_DOWN_MASK) }),
    IMPORT_DATA("menu.file.import", ShortcutGroup.MENU, { commandShortcut(VK_I, it, SHIFT_DOWN_MASK) }),
    EXPORT_DATA("menu.file.export", ShortcutGroup.MENU, { commandShortcut(VK_X, it, SHIFT_DOWN_MASK) }),
    OPEN_SHORTCUT_SETTINGS("menu.settings.shortcut", ShortcutGroup.MENU, { commandShortcut(VK_COMMA, it) }),
    TOGGLE_DESCRIPTION("menu.view.description", ShortcutGroup.MENU, { keyStroke(VK_E, ALT_DOWN_MASK) }),
    TOGGLE_GROUP_FILTER("menu.view.showInputGroup", ShortcutGroup.MENU, { keyStroke(VK_G, ALT_DOWN_MASK) }),
    TOGGLE_ALWAYS_ON_TOP("menu.view.alwaysOnTop", ShortcutGroup.MENU, { commandShortcut(VK_F, it, SHIFT_DOWN_MASK) }),
    SHOW_ABOUT("menu.help.about", ShortcutGroup.MENU, { keyStroke(VK_F1) }),
    RUN_SELECTED_LINK("shortcut.action.runSelectedLink", ShortcutGroup.TABLE, { keyStroke(VK_ENTER) }),
    FOCUS_SEARCH("shortcut.action.focusSearch", ShortcutGroup.TABLE, { keyStroke(VK_ESCAPE) }),
    DELETE_SELECTED_LINK("shortcut.action.deleteSelectedLink", ShortcutGroup.TABLE, { keyStroke(VK_DELETE) }),
    MOVE_FOCUS_TO_DETAIL("shortcut.action.moveFocusToDetail", ShortcutGroup.TABLE, { keyStroke(VK_TAB) }),
    COPY_FOLDER_FROM_TABLE("shortcut.action.copyFolderFromTable", ShortcutGroup.TABLE, { commandShortcut(VK_C, it) }),
    OPEN_FOLDER_FROM_TABLE("shortcut.action.openFolderFromTable", ShortcutGroup.TABLE, { commandShortcut(VK_O, it, SHIFT_DOWN_MASK) }),
    ;

    val defaultKeyStroke: KeyStroke
        get() = defaultKeyStroke()

    fun defaultKeyStroke(platform: ShortcutPlatform = ShortcutPlatform.current()): KeyStroke {
        return defaultKeyStrokeFactory(platform)
    }
}

/** stored for an action the user left without a shortcut. */
private const val UNBOUND = "None"

/** shortcut of every action; null for an action left without one. */
class ShortcutSettings private constructor(
    private val bindings: EnumMap<ShortcutAction, KeyStroke?>,
) {

    operator fun get(action: ShortcutAction): KeyStroke? {
        return if (bindings.containsKey(action)) bindings[action] else action.defaultKeyStroke
    }

    /** the shortcut as text, empty when there is none. */
    fun displayText(action: ShortcutAction): String {
        return get(action)?.displayText ?: ""
    }

    fun matches(action: ShortcutAction, event: KeyEvent): Boolean {
        return get(action)?.matches(event) ?: false
    }

    fun toMutableBindings(): EnumMap<ShortcutAction, KeyStroke?> {
        return EnumMap(bindings)
    }

    /** bindings by action name, in the `Ctrl+Shift+N` form the JavaFX version stored as well. */
    fun toConfigMap(): MutableMap<String, String> {
        return bindings.entries
            .associate { (action, keyStroke) -> action.name to (keyStroke?.displayText ?: UNBOUND) }
            .toMutableMap()
    }

    companion object {
        fun load(
            configured: Map<String, String>?,
            platform: ShortcutPlatform = ShortcutPlatform.current(),
        ): ShortcutSettings {
            val bindings = EnumMap<ShortcutAction, KeyStroke?>(ShortcutAction::class.java)
            ShortcutAction.entries.forEach { action ->
                val text = configured?.get(action.name)
                bindings[action] = if (text == UNBOUND) null else text.toKeyStroke() ?: action.defaultKeyStroke(platform)
            }
            return ShortcutSettings(bindings)
        }

        /** @param bindings shortcut by action: a null value leaves the action without one, a missing action takes its default */
        fun fromBindings(bindings: Map<ShortcutAction, KeyStroke?>): ShortcutSettings {
            val copied = EnumMap<ShortcutAction, KeyStroke?>(ShortcutAction::class.java)
            ShortcutAction.entries.forEach { action ->
                copied[action] = if (bindings.containsKey(action)) bindings[action] else action.defaultKeyStroke
            }
            return ShortcutSettings(copied)
        }
    }
}

private fun commandShortcut(
    keyCode: Int,
    platform: ShortcutPlatform,
    modifiers: Int = 0,
): KeyStroke {
    return keyStroke(keyCode, platform.commandMask or modifiers)
}

fun findDuplicateShortcuts(bindings: Map<ShortcutAction, KeyStroke?>): List<List<ShortcutAction>> {
    return bindings.entries
        .filter { it.value != null }
        .groupBy { it.value }
        .values
        .filter { it.size > 1 }
        .map { entries -> entries.map { it.key } }
}

/**
 * keys the text inputs of the main window use themselves: editing and the search history (Alt+arrows).
 * Ctrl is Command on macOS; both are listed, as the platform of a key stroke is unknown here.
 */
private val TEXT_INPUT_KEYS: Set<KeyStroke> = buildSet {
    for (primary in listOf(CTRL_DOWN_MASK, META_DOWN_MASK)) {
        listOf(VK_A, VK_C, VK_V, VK_X, VK_Y, VK_Z, VK_INSERT, VK_LEFT, VK_RIGHT, VK_HOME, VK_END, VK_BACK_SPACE, VK_DELETE, VK_ENTER)
            .forEach { add(keyStroke(it, primary)) }
        listOf(VK_Z, VK_LEFT, VK_RIGHT, VK_HOME, VK_END, VK_ENTER).forEach { add(keyStroke(it, primary or SHIFT_DOWN_MASK)) }
    }
    listOf(VK_DOWN, VK_UP, VK_LEFT, VK_RIGHT).forEach { add(keyStroke(it, ALT_DOWN_MASK)) }
}

/**
 * why [keyStroke] cannot be the shortcut of [action], as a message key; null when it can.
 *
 * table shortcuts only apply while the table has the focus, so any key will do. the others are caught
 * before a focused text input sees the key: they need Ctrl, Alt or Command, or a function key, and must
 * leave the keys of the text inputs alone.
 */
fun shortcutRestriction(action: ShortcutAction, keyStroke: KeyStroke): String? {
    if (action.group == ShortcutGroup.TABLE) return null
    val isFunctionKey = keyStroke.keyCode in VK_F1..VK_F12 || keyStroke.keyCode in VK_F13..VK_F24
    val hasCommandModifier = keyStroke.modifiersEx and (CTRL_DOWN_MASK or ALT_DOWN_MASK or META_DOWN_MASK) != 0
    return when {
        !isFunctionKey && !hasCommandModifier -> "msg.error.shortcut.modifier"
        keyStroke in TEXT_INPUT_KEYS -> "msg.error.shortcut.reserved"
        else -> null
    }
}
