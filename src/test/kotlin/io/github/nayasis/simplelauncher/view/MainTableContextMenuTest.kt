package io.github.nayasis.simplelauncher.view

import io.github.nayasis.kotlin.basica.model.Messages
import io.github.nayasis.kotlin.basica.model.Messages.Companion.loadMessages
import io.github.nayasis.kotlin.swing.misc.Localization
import io.github.nayasis.kotlin.swing.misc.runUiAndWait
import io.github.nayasis.simplelauncher.service.Languages
import io.github.nayasis.simplelauncher.service.ShortcutAction
import io.github.nayasis.simplelauncher.service.ShortcutSettings
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.util.Locale
import javax.swing.JMenuItem

internal class MainTableContextMenuTest {

    @Test
    fun contextMenuShouldContainOnlyItemActionsWithConfiguredShortcuts() = runUiAndWait<Unit> {
        val shortcuts = ShortcutSettings.load(
            mapOf(
                ShortcutAction.RUN_SELECTED_LINK.name to "Alt+R",
                ShortcutAction.COPY_FOLDER_FROM_TABLE.name to "Alt+C",
                ShortcutAction.OPEN_FOLDER_FROM_TABLE.name to "Alt+O",
                ShortcutAction.DELETE_SELECTED_LINK.name to "Alt+DELETE",
                ShortcutAction.TOGGLE_DESCRIPTION.name to "Alt+E",
            )
        )
        val performed = mutableListOf<ShortcutAction>()
        var showDetailCount = 0

        val menu = createTableItemContextMenu(
            { shortcuts },
            { performed += it },
            { showDetailCount++ },
        )
        val items = menu.components.filterIsInstance<JMenuItem>()
        val actionItems = items.filter { it.getClientProperty(CONTEXT_MENU_ACTION_KEY) is ShortcutAction }

        actionItems.map { it.getClientProperty(CONTEXT_MENU_ACTION_KEY) } shouldContainExactly listOf(
            ShortcutAction.RUN_SELECTED_LINK,
            ShortcutAction.COPY_FOLDER_FROM_TABLE,
            ShortcutAction.OPEN_FOLDER_FROM_TABLE,
            ShortcutAction.DELETE_SELECTED_LINK,
        )
        actionItems.map { it.accelerator } shouldContainExactly listOf(
            shortcuts[ShortcutAction.RUN_SELECTED_LINK],
            shortcuts[ShortcutAction.COPY_FOLDER_FROM_TABLE],
            shortcuts[ShortcutAction.OPEN_FOLDER_FROM_TABLE],
            shortcuts[ShortcutAction.DELETE_SELECTED_LINK],
        )

        val showDetailItem = items.firstOrNull { it.name == CONTEXT_MENU_SHOW_DETAIL_ID }
        showDetailItem.shouldNotBeNull()
        showDetailItem.accelerator shouldBe shortcuts[ShortcutAction.TOGGLE_DESCRIPTION]

        actionItems.first().doClick()
        performed shouldContainExactly listOf(ShortcutAction.RUN_SELECTED_LINK)

        showDetailItem.doClick()
        showDetailCount shouldBe 1
    }

    @Test
    fun contextMenuFollowsALanguageSwitch() = runUiAndWait<Unit> {
        val saved = Messages.defaultLocale
        try {
            Languages.apply(Locale.ENGLISH)
            val menu = createTableItemContextMenu({ ShortcutSettings.load(emptyMap()) }, {}, {})
            val showDetailItem = menu.components.filterIsInstance<JMenuItem>().first { it.name == CONTEXT_MENU_SHOW_DETAIL_ID }
            showDetailItem.text shouldBe "Details"

            Languages.apply(Locale.JAPANESE)
            Localization.refresh()
            showDetailItem.text shouldBe "詳細"
        } finally {
            Languages.apply(saved)
            Localization.refresh()
        }
    }

    companion object {

        @JvmStatic
        @BeforeAll
        fun loadMessages() {
            "/message/**.prop".loadMessages()
        }

    }

}
