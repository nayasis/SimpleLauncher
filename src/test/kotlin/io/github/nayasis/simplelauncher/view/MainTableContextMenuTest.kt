package io.github.nayasis.simplelauncher.view

import io.github.nayasis.kotlin.basica.model.Messages.Companion.loadMessages
import io.github.nayasis.simplelauncher.service.ShortcutAction
import io.github.nayasis.simplelauncher.service.ShortcutSettings
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import javafx.application.Platform
import javafx.embed.swing.JFXPanel
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

internal class MainTableContextMenuTest {

    @Test
    fun contextMenuShouldContainOnlyItemActionsWithConfiguredShortcuts() = runOnFxThread {
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
        val actionItems = menu.items.filter { it.userData is ShortcutAction }

        actionItems.map { it.userData as ShortcutAction } shouldContainExactly listOf(
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

        val showDetailItem = menu.items.firstOrNull { it.id == CONTEXT_MENU_SHOW_DETAIL_ID }
        showDetailItem.shouldNotBeNull()
        showDetailItem.accelerator shouldBe shortcuts[ShortcutAction.TOGGLE_DESCRIPTION]

        actionItems.first().fire()
        performed shouldContainExactly listOf(ShortcutAction.RUN_SELECTED_LINK)

        showDetailItem.fire()
        showDetailCount shouldBe 1
    }

    companion object {

        @JvmStatic
        @BeforeAll
        fun initToolkit() {
            "/message/**.prop".loadMessages()
            JFXPanel()
        }

        private fun runOnFxThread(block: () -> Unit) {
            if (Platform.isFxApplicationThread()) {
                block()
                return
            }

            var failure: Throwable? = null
            val latch = CountDownLatch(1)
            Platform.runLater {
                try {
                    block()
                } catch (e: Throwable) {
                    failure = e
                } finally {
                    latch.countDown()
                }
            }

            latch.await(5, TimeUnit.SECONDS).shouldBeTrue()
            failure?.let { throw it }
        }

    }

}
