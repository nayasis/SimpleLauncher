package io.github.nayasis.simplelauncher.view

import io.github.nayasis.kotlin.basica.model.Messages.Companion.loadMessages
import io.github.nayasis.kotlin.swing.app.SwingApp
import io.github.nayasis.kotlin.swing.misc.runUiAndWait
import io.github.nayasis.simplelauncher.service.LinkExecutor
import io.github.nayasis.simplelauncher.service.LinkService
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.awt.Container
import java.awt.Dimension
import java.time.LocalDateTime
import javax.swing.JFrame
import javax.swing.JLabel
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

internal class MainLayoutTest {

    @Test
    fun lastUsedColumnCanShrinkAndShowsDateOnlyUntilTheFullTimestampFits() = runUiAndWait<Unit> {
        val table = Main().tableMain
        val index = table.columnModel.getColumnIndex("colLastUsedDt")
        val column = table.columnModel.getColumn(index)
        val date = LocalDateTime.of(2026, 10, 27, 1, 2, 3)
        val full = "2026-10-27 01:02:03"
        val metrics = table.getFontMetrics(table.font)
        fun render(value: LocalDateTime?): JLabel =
            column.cellRenderer.getTableCellRendererComponent(table, value, false, false, 0, index) as JLabel

        column.width = 50
        assertEquals(50, column.width)
        column.width = metrics.stringWidth("2026-10-27") + 8
        assertEquals("2026-10-27", render(date).text)
        assertEquals(full, render(date).toolTipText)

        column.width = metrics.stringWidth(full) + 8
        assertEquals(full, render(date).text)
        column.width = metrics.stringWidth("2026-10-27") + 8
        assertEquals("2026-10-27", render(date).text)
        assertEquals("", render(null).text)
        assertNull(render(null).toolTipText)
    }

    @Test
    fun keywordRemainsWiderThanGroupInSmallAndLargeWindows() = runUiAndWait<Unit> {
        val main = Main()
        val frame = JFrame().apply { contentPane = main.root }
        try {
            frame.addNotify()
            listOf(430, 750, 1000).forEach { width ->
                frame.size = Dimension(width, 450)
                frame.validate()
                assertTrue(main.inputKeyword.width > main.inputGroup.width,
                    "at $width px, keyword ${main.inputKeyword.width} must be wider than group ${main.inputGroup.width}")
            }
        } finally {
            frame.dispose()
        }
    }

    @Test
    fun editorActionsStayFullyVisibleInAShortWindow() = runUiAndWait<Unit> {
        val main = Main()
        val frame = JFrame().apply { contentPane = main.root; size = Dimension(750, 260) }
        try {
            frame.addNotify()
            frame.validate()
            main.descPanel.size = Dimension(300, main.descPanel.height)
            fun layout(container: Container) {
                container.doLayout()
                container.components.filterIsInstance<Container>().forEach(::layout)
            }
            layout(main.descPanel)
            listOf(main.buttonNew, main.buttonDelete, main.buttonCopy, main.buttonSave).forEach { button ->
                val bounds = javax.swing.SwingUtilities.convertRectangle(button.parent, button.bounds, main.descPanel)
                assertTrue(bounds.y >= 0 && bounds.y + bounds.height <= main.descPanel.height,
                    "${button.text} must remain inside the editor: $bounds, editor height ${main.descPanel.height}")
                assertTrue(bounds.height >= button.preferredSize.height)
            }
        } finally {
            frame.dispose()
        }
    }

    companion object {
        @JvmStatic
        @BeforeAll
        fun prepare() {
            "/message/**.prop".loadMessages()
            SwingApp.ctx.create(LinkService::class)
            SwingApp.ctx.create(LinkExecutor::class)
        }
    }
}
