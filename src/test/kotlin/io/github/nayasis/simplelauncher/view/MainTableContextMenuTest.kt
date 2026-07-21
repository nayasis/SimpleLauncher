package io.github.nayasis.simplelauncher.view

import io.github.nayasis.kotlin.basica.model.Messages.Companion.loadMessages
import io.github.nayasis.simplelauncher.service.ShortcutAction
import io.github.nayasis.simplelauncher.service.ShortcutSettings
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.collections.shouldContainExactly
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
                ShortcutAction.DELETE_SELECTED_LINK.name to "Alt+DELETE",
            )
        )
        val performed = mutableListOf<ShortcutAction>()

        val menu = createTableItemContextMenu({ shortcuts }) { performed += it }
        val actionItems = menu.items.filter { it.userData is ShortcutAction }

        actionItems.map { it.userData as ShortcutAction } shouldContainExactly listOf(
            ShortcutAction.RUN_SELECTED_LINK,
            ShortcutAction.COPY_FOLDER_FROM_TABLE,
            ShortcutAction.DELETE_SELECTED_LINK,
        )
        actionItems.map { it.accelerator } shouldContainExactly listOf(
            shortcuts[ShortcutAction.RUN_SELECTED_LINK],
            shortcuts[ShortcutAction.COPY_FOLDER_FROM_TABLE],
            shortcuts[ShortcutAction.DELETE_SELECTED_LINK],
        )

        actionItems.first().fire()
        performed shouldContainExactly listOf(ShortcutAction.RUN_SELECTED_LINK)
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
