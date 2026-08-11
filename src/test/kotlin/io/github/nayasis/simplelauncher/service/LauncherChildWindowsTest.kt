package io.github.nayasis.simplelauncher.service

import io.kotest.matchers.shouldBe
import javafx.geometry.Point2D
import javafx.geometry.Rectangle2D
import org.junit.jupiter.api.Test

internal class LauncherChildWindowsTest {

    @Test
    fun `uses saved progress dialog position when it is visible`() {
        val saved = Point2D(120.0, 80.0)

        val position = resolveProgressDialogPosition(
            lastPosition = saved,
            dialogWidth = 300.0,
            dialogHeight = 120.0,
            mainBounds = Rectangle2D(0.0, 0.0, 1920.0, 1080.0),
            isShownOnScreen = { rect -> rect.minX >= 0.0 && rect.maxX <= 1920.0 && rect.minY >= 0.0 && rect.maxY <= 1080.0 },
        )

        position shouldBe saved
    }

    @Test
    fun `centers progress dialog when there is no saved position`() {
        val position = resolveProgressDialogPosition(
            lastPosition = null,
            dialogWidth = 300.0,
            dialogHeight = 120.0,
            mainBounds = Rectangle2D(100.0, 50.0, 1000.0, 800.0),
            isShownOnScreen = { true },
        )

        position shouldBe Point2D(450.0, 390.0)
    }

    @Test
    fun `centers progress dialog when saved position is off screen`() {
        val position = resolveProgressDialogPosition(
            lastPosition = Point2D(5000.0, 3000.0),
            dialogWidth = 300.0,
            dialogHeight = 120.0,
            mainBounds = Rectangle2D(0.0, 0.0, 1920.0, 1080.0),
            isShownOnScreen = { rect -> rect.minX >= 0.0 && rect.maxX <= 1920.0 && rect.minY >= 0.0 && rect.maxY <= 1080.0 },
        )

        position shouldBe Point2D(810.0, 480.0)
    }
}
