package io.github.nayasis.simplelauncher.view

import io.github.nayasis.kotlin.basica.model.Messages.Companion.loadMessages
import io.github.nayasis.kotlin.swing.misc.runUiAndWait
import io.github.nayasis.simplelauncher.service.ShortcutAction
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.awt.Container
import java.awt.Dimension
import javax.swing.JComponent
import javax.swing.JTextArea
import javax.swing.JTextField
import kotlin.test.assertTrue

internal class ShortcutEditorTest {

    @Test
    fun buildContentShouldConstructAnEditorPerAction() = runUiAndWait<Unit> {
        val content = ShortcutEditor().buildContent()

        content.components.isNotEmpty().shouldBeTrue()
        val editors = content.descendants().filterIsInstance<JTextField>().mapNotNull { it.name }
        editors.toSet() shouldBe ShortcutAction.entries.map { "shortcut-${it.name}" }.toSet()
    }

    @Test
    fun helpBoxGrowsWhenTextWrapsAndShrinksForOneLine() = runUiAndWait<Unit> {
        val content = ShortcutEditor().buildContent()
        val text = content.descendants().filterIsInstance<JTextArea>().single()
        val help = text.parent as JComponent
        text.text = "A helpful message about recording keyboard shortcuts. ".repeat(3)

        fun layout(width: Int) {
            content.size = Dimension(width, 1000)
            content.invalidate()
            content.doLayout()
            help.doLayout()
            content.invalidate()
            content.doLayout()
            help.doLayout()
        }

        layout(360)
        val wrappedHeight = help.preferredSize.height
        assertTrue(text.preferredSize.height >= text.getFontMetrics(text.font).height * 2)
        assertTrue(help.height >= wrappedHeight, "wrapped help must not be capped at its initial one-line height")

        text.text = "Short help."
        layout(360)
        val singleLineHeight = help.preferredSize.height
        assertTrue(singleLineHeight < wrappedHeight)
        help.height shouldBe singleLineHeight
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
