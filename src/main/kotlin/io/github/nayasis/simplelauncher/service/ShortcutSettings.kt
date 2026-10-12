package io.github.nayasis.simplelauncher.service

import io.github.nayasis.kotlin.swing.misc.displayText
import io.github.nayasis.kotlin.swing.misc.keyStroke
import io.github.nayasis.kotlin.swing.misc.matches
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
    private val defaultKeyStrokeFactory: (ShortcutPlatform) -> KeyStroke,
) {
    SAVE_DETAIL("btn.save", ShortcutGroup.GENERAL, { commandShortcut(VK_S, it) }),
    COPY_DETAIL("btn.copy", ShortcutGroup.GENERAL, { commandShortcut(VK_D, it) }),
    CREATE_DETAIL("btn.new", ShortcutGroup.GENERAL, { commandShortcut(VK_N, it) }),
    ADD_FILE("shortcut.action.addFile", ShortcutGroup.GENERAL, { commandShortcut(VK_N, it, SHIFT_DOWN_MASK) }),
    OPEN_FOLDER("btn.open.folder", ShortcutGroup.GENERAL, { commandShortcut(VK_O, it) }),
    COPY_FOLDER("btn.copy.folder", ShortcutGroup.GENERAL, { commandShortcut(VK_C, it, SHIFT_DOWN_MASK) }),
    CHANGE_ICON("shortcut.action.changeIcon", ShortcutGroup.GENERAL, { commandShortcut(VK_I, it) }),
    DELETE_DETAIL("shortcut.action.deleteDetail", ShortcutGroup.GENERAL, { keyStroke(VK_DELETE, SHIFT_DOWN_MASK) }),
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

class ShortcutSettings private constructor(
    private val bindings: EnumMap<ShortcutAction, KeyStroke>,
) {

    operator fun get(action: ShortcutAction): KeyStroke {
        return bindings[action] ?: action.defaultKeyStroke
    }

    fun displayText(action: ShortcutAction): String {
        return get(action).displayText
    }

    fun matches(action: ShortcutAction, event: KeyEvent): Boolean {
        return get(action).matches(event)
    }

    fun toMutableBindings(): EnumMap<ShortcutAction, KeyStroke> {
        return EnumMap(bindings)
    }

    /** bindings by action name, in the `Ctrl+Shift+N` form the JavaFX version stored as well. */
    fun toConfigMap(): MutableMap<String, String> {
        return bindings.entries
            .associate { (action, keyStroke) -> action.name to keyStroke.displayText }
            .toMutableMap()
    }

    companion object {
        fun load(
            configured: Map<String, String>?,
            platform: ShortcutPlatform = ShortcutPlatform.current(),
        ): ShortcutSettings {
            val bindings = EnumMap<ShortcutAction, KeyStroke>(ShortcutAction::class.java)
            ShortcutAction.entries.forEach { action ->
                bindings[action] = configured?.get(action.name).toKeyStroke() ?: action.defaultKeyStroke(platform)
            }
            return ShortcutSettings(bindings)
        }

        fun fromBindings(bindings: Map<ShortcutAction, KeyStroke>): ShortcutSettings {
            val copied = EnumMap<ShortcutAction, KeyStroke>(ShortcutAction::class.java)
            ShortcutAction.entries.forEach { action ->
                copied[action] = bindings[action] ?: action.defaultKeyStroke
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
    val primary = when (platform) {
        ShortcutPlatform.MAC -> META_DOWN_MASK
        ShortcutPlatform.WINDOWS_LINUX -> CTRL_DOWN_MASK
    }
    return keyStroke(keyCode, primary or modifiers)
}

fun findDuplicateShortcuts(bindings: Map<ShortcutAction, KeyStroke>): List<List<ShortcutAction>> {
    return bindings.entries
        .groupBy { it.value }
        .values
        .filter { it.size > 1 }
        .map { entries -> entries.map { it.key } }
}
