package io.github.nayasis.simplelauncher.view

import io.github.nayasis.simplelauncher.service.SearchToken
import io.github.nayasis.simplelauncher.service.SearchTokenKind
import io.kotest.matchers.shouldBe
import javafx.application.Platform
import javafx.embed.swing.JFXPanel
import javafx.scene.control.Button
import javafx.scene.control.OverrunStyle
import javafx.scene.input.KeyCode
import javafx.scene.input.KeyEvent
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

internal class SearchTokenFieldTest {

    @Test
    fun shiftEnterTurnsAndOrTextIntoOperatorTokens() = runOnFxThread {
        val andField = SearchTokenField()
        val orField = SearchTokenField()

        andField.text = "AND"
        pressShiftEnter(andField)

        andField.tokens() shouldBe listOf(SearchToken(SearchTokenKind.AND, "and"))

        orField.text = "OR"
        pressShiftEnter(orField)

        orField.tokens() shouldBe listOf(SearchToken(SearchTokenKind.OR, "or"))
    }

    @Test
    fun controlEnterTurnsAndOrTextIntoTermTokens() = runOnFxThread {
        val andField = SearchTokenField()
        val orField = SearchTokenField()

        andField.text = "AND"
        pressEnter(andField, control = true)

        andField.tokens() shouldBe listOf(SearchToken(SearchTokenKind.TERM, "AND"))

        orField.text = "OR"
        pressEnter(orField, control = true)

        orField.tokens() shouldBe listOf(SearchToken(SearchTokenKind.TERM, "OR"))
    }

    @Test
    fun operatorAndOrTokenButtonsUseCompactOperatorWordStyle() = runOnFxThread {
        val field = SearchTokenField()

        field.text = "AND"
        pressShiftEnter(field)

        tokenButtons(field).first().styleClass.contains("operator-word") shouldBe true
    }

    @Test
    fun literalAndOrTermTokenButtonsKeepNormalTokenStyle() = runOnFxThread {
        val field = SearchTokenField()

        field.text = "AND"
        pressEnter(field, control = true)

        tokenButtons(field).first().styleClass.contains("operator-word") shouldBe false
    }

    @Test
    fun tokenFieldClipsOverflowInsteadOfWrappingOutsideInputBox() = runOnFxThread {
        val field = SearchTokenField()

        (field.clip != null) shouldBe true
    }

    @Test
    fun tokenButtonsClipTextInsteadOfShowingEllipsis() = runOnFxThread {
        val field = SearchTokenField()

        field.text = "very-long-token"
        pressEnter(field, control = true)

        tokenButtons(field).first().textOverrun shouldBe OverrunStyle.CLIP
    }

    private fun pressShiftEnter(field: SearchTokenField) {
        pressEnter(field, shift = true)
    }

    private fun tokenButtons(field: SearchTokenField): List<Button> {
        return field.children.filterIsInstance<Button>()
    }

    private fun pressEnter(
        field: SearchTokenField,
        shift: Boolean = false,
        control: Boolean = false,
    ) {
        field.input.fireEvent(
            KeyEvent(
                KeyEvent.KEY_PRESSED,
                "",
                "",
                KeyCode.ENTER,
                shift,
                control,
                false,
                false,
            ),
        )
    }

    companion object {

        @JvmStatic
        @BeforeAll
        fun initToolkit() {
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
            latch.await(5, TimeUnit.SECONDS) shouldBe true
            failure?.let { throw it }
        }

    }

}
