package io.github.nayasis.simplelauncher.view

import io.github.nayasis.kotlin.basica.model.Messages.Companion.loadMessages
import io.github.nayasis.kotlin.swing.misc.runUiAndWait
import io.github.nayasis.simplelauncher.service.ShortcutAction
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.awt.Container
import javax.swing.JTextField

internal class ShortcutEditorTest {

    @Test
    fun buildContentShouldConstructAnEditorPerAction() = runUiAndWait<Unit> {
        val content = ShortcutEditor().buildContent()

        content.components.isNotEmpty().shouldBeTrue()
        val editors = content.descendants().filterIsInstance<JTextField>().mapNotNull { it.name }
        editors.toSet() shouldBe ShortcutAction.entries.map { "shortcut-${it.name}" }.toSet()
    }

    private fun Container.descendants(): List<java.awt.Component> =
        components.flatMap { listOf(it) + ((it as? Container)?.descendants() ?: emptyList()) }

    companion object {

        @JvmStatic
        @BeforeAll
        fun loadMessages() {
            "/message/**.prop".loadMessages()
        }

    }

}
