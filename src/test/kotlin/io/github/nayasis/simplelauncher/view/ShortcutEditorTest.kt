package io.github.nayasis.simplelauncher.view

import io.github.nayasis.kotlin.basica.model.Messages.Companion.loadMessages
import io.kotest.matchers.booleans.shouldBeTrue
import javafx.application.Platform
import javafx.embed.swing.JFXPanel
import javafx.scene.layout.VBox
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

internal class ShortcutEditorTest {

    @Test
    fun buildContentShouldConstructShortcutGroupsWithoutDuplicateChildren() = runOnFxThread {
        val editor = ShortcutEditor()
        val method = ShortcutEditor::class.java.getDeclaredMethod("buildContent")
        method.isAccessible = true

        val content = method.invoke(editor) as VBox

        (content.children.isNotEmpty()).shouldBeTrue()
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
