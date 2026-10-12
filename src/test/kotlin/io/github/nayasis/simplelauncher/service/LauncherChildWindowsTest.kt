package io.github.nayasis.simplelauncher.service

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import java.awt.Point
import java.awt.Rectangle

internal class LauncherChildWindowsTest {

    private val fullHd = Rectangle(0, 0, 1920, 1080)

    @Test
    fun `uses saved progress dialog position when it is visible`() {
        val saved = Point(120, 80)

        val position = resolveProgressDialogPosition(
            lastPosition = saved,
            dialogWidth = 300,
            dialogHeight = 120,
            mainBounds = fullHd,
            isShownOnScreen = { rect -> fullHd.contains(rect) },
        )

        position shouldBe saved
    }

    @Test
    fun `centers progress dialog when there is no saved position`() {
        val position = resolveProgressDialogPosition(
            lastPosition = null,
            dialogWidth = 300,
            dialogHeight = 120,
            mainBounds = Rectangle(100, 50, 1000, 800),
            isShownOnScreen = { true },
        )

        position shouldBe Point(450, 390)
    }

    @Test
    fun `centers progress dialog when saved position is off screen`() {
        val position = resolveProgressDialogPosition(
            lastPosition = Point(5000, 3000),
            dialogWidth = 300,
            dialogHeight = 120,
            mainBounds = fullHd,
            isShownOnScreen = { rect -> fullHd.contains(rect) },
        )

        position shouldBe Point(810, 480)
    }
}
