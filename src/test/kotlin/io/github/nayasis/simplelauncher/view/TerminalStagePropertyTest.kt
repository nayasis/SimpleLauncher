package io.github.nayasis.simplelauncher.view

import io.github.nayasis.kotlin.javafx.property.InsetProperty
import io.github.nayasis.kotlin.javafx.property.StageProperty
import io.github.nayasis.kotlin.javafx.stage.MaximizedProperty
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertSame
import kotlin.test.assertTrue

class TerminalStagePropertyTest {

    @Test
    fun `clear stale maximized state when previous boundary was never captured`() {
        val property = StageProperty(
            inset = InsetProperty(x = -723, y = 247, width = 670, height = 411),
            maximized = true,
            previousBoundary = MaximizedProperty().apply {
                maximized = true
                boundary = InsetProperty()
            },
        )

        val sanitized = property.sanitizeTerminalStageProperty()

        assertSame(property, sanitized)
        assertFalse(sanitized.maximized)
        assertFalse(sanitized.previousBoundary!!.maximized)
    }

    @Test
    fun `keep maximized state when previous boundary was captured`() {
        val property = StageProperty(
            inset = InsetProperty(x = -10, y = -10, width = 1940, height = 1040),
            maximized = true,
            previousBoundary = MaximizedProperty().apply {
                maximized = true
                boundary = InsetProperty(x = -723, y = 247, width = 670, height = 411)
            },
        )

        val sanitized = property.sanitizeTerminalStageProperty()

        assertSame(property, sanitized)
        assertTrue(sanitized.maximized)
        assertTrue(sanitized.previousBoundary!!.maximized)
    }

}
