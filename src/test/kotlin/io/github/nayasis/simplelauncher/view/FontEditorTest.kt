package io.github.nayasis.simplelauncher.view

import io.github.nayasis.kotlin.basica.model.Messages
import io.github.nayasis.kotlin.basica.model.Messages.Companion.loadMessages
import io.github.nayasis.kotlin.basica.reflection.Reflector
import io.github.nayasis.kotlin.basica.reflection.toObject
import io.github.nayasis.kotlin.swing.misc.runUiAndWait
import io.github.nayasis.simplelauncher.service.Config
import io.github.nayasis.simplelauncher.service.Languages
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.awt.Component
import java.awt.Container
import java.awt.Font
import java.awt.Window
import javax.swing.JButton
import javax.swing.JComboBox
import javax.swing.JDialog
import javax.swing.JLabel
import javax.swing.JSlider
import javax.swing.JSpinner
import javax.swing.Timer
import javax.swing.UIManager
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

internal class FontEditorTest {

    @Test
    fun sliderAndTypedSizeUpdateEveryRegisteredLanguageImmediately() = runUiAndWait<Unit> {
        val content = FontEditor(AppFonts.font(Font.DIALOG, 13)).buildContent()
        val slider = content.named<JSlider>("fontSizeSlider")
        val spinner = content.named<JSpinner>("fontSizeInput")
        val samples = content.descendants().filterIsInstance<JLabel>().filter { it.name?.startsWith("fontPreview-") == true }

        assertEquals(Languages.available.map { "fontPreview-${it.toLanguageTag()}" }.toSet(), samples.map { it.name }.toSet())
        Languages.available.forEach { locale ->
            val sample = content.named<JLabel>("fontPreview-${locale.toLanguageTag()}")
            assertEquals(Messages["font.preview.sample", locale], sample.text)
            assertNotEquals("font.preview.sample", sample.text)
        }

        slider.value = 24
        assertEquals(24, spinner.value)
        assertTrue(samples.all { it.font.size == 24 })

        (spinner.editor as JSpinner.NumberEditor).textField.text = "31"
        assertEquals(31, spinner.value)
        assertEquals(31, slider.value)
        assertTrue(samples.all { it.font.size == 31 })

        content.named<JComboBox<*>>("fontFamily").selectedItem = Font.SERIF
        assertTrue(samples.all { it.font.family == Font.SERIF })
        assertTrue(samples.all { it.font.size == 31 })

        slider.value = AppFonts.MIN_SIZE
        assertEquals(AppFonts.MIN_SIZE, spinner.value)
        slider.value = AppFonts.MAX_SIZE
        assertEquals(AppFonts.MAX_SIZE, spinner.value)
        assertTrue(samples.all { it.font.size == AppFonts.MAX_SIZE })
    }

    @Test
    fun savesTheEditedFontButCancelLeavesTheApplicationUnchanged() = runUiAndWait<Unit> {
        val before = AppFonts.current()
        val saved = editDialog { dialog ->
            dialog.named<JComboBox<*>>("fontFamily").selectedItem = Font.MONOSPACED
            dialog.named<JSpinner>("fontSizeInput").value = 22
            dialog.named<JButton>("fontSave").doClick()
        }
        assertNotNull(saved)
        assertEquals(Font.MONOSPACED, saved.family)
        assertEquals(22, saved.size)
        assertEquals(before, AppFonts.current())

        val canceled = editDialog { dialog ->
            dialog.named<JSlider>("fontSizeSlider").value = 35
            dialog.named<JButton>("fontCancel").doClick()
        }
        assertNull(canceled)
        assertEquals(before, AppFonts.current())
    }

    @Test
    fun invalidTypedSizeDoesNotCloseOrSaveTheDialog() = runUiAndWait<Unit> {
        val result = editDialog { dialog ->
            val spinner = dialog.named<JSpinner>("fontSizeInput")
            (spinner.editor as JSpinner.NumberEditor).textField.text = "999"
            dialog.named<JButton>("fontSave").doClick()
            assertTrue(dialog.isShowing)
            assertEquals("error", spinner.getClientProperty("JComponent.outline"))
        }
        assertNull(result)
    }

    @Test
    fun savedFontSurvivesThemeChangesAndConfigRoundTrip() = runUiAndWait<Unit> {
        val lookAndFeel = UIManager.getLookAndFeel()
        val override = UIManager.get("defaultFont")
        try {
            AppTheme.INTELLIJ.install()
            val config = Config().apply { fontFamily = Font.SERIF; fontSize = 23 }
            val restored = Reflector.toJson(config).toObject<Config>()
            assertEquals(config.fontFamily, restored.fontFamily)
            assertEquals(config.fontSize, restored.fontSize)
            AppFonts.install(restored.fontFamily, restored.fontSize)
            assertEquals(Font.SERIF, JLabel().font.family)
            assertEquals(23, JLabel().font.size)

            AppTheme.DARK.apply()
            assertEquals(Font.SERIF, JLabel().font.family)
            assertEquals(23, JLabel().font.size)

            // The same setup order is used at startup; absent settings use the theme's font.
            UIManager.put("defaultFont", null)
            AppTheme.INTELLIJ.install()
            AppFonts.install(restored.fontFamily, restored.fontSize)
            assertEquals(Font.SERIF, AppFonts.current().family)
            assertEquals(23, AppFonts.current().size)
            AppFonts.install(null, null)
            assertEquals(UIManager.getLookAndFeelDefaults().getFont("defaultFont"), AppFonts.current())
            assertNull("{}".toObject<Config>().fontFamily)
            assertNull("{}".toObject<Config>().fontSize)
        } finally {
            UIManager.setLookAndFeel(lookAndFeel)
            UIManager.put("defaultFont", override)
        }
    }

    @Test
    fun everyBundleHasTheSameKeysIncludingTheFontPreview() {
        val keySets = Languages.available.map { locale ->
            val suffix = locale.language + locale.country.takeIf { it.isNotEmpty() }.orEmpty().let { if (it.isEmpty()) "" else "_$it" }
            javaClass.getResource("/message/localization.$suffix.prop")!!.readText().lineSequence()
                .filter { it.isNotBlank() && !it.startsWith('#') && '=' in it }
                .map { it.substringBefore('=').trim() }.toSet()
        }
        keySets.forEach { assertEquals(keySets.first(), it) }
    }

    private fun editDialog(action: (JDialog) -> Unit): Font? {
        var failure: Throwable? = null
        val timer = Timer(80) {
            val dialog = Window.getWindows().filterIsInstance<JDialog>().first { it.name == "fontDialog" && it.isShowing }
            try {
                action(dialog)
            } catch (e: Throwable) {
                failure = e
            } finally {
                dialog.dispose()
            }
        }.apply { isRepeats = false }
        timer.start()
        val result = FontEditor(AppFonts.font(Font.DIALOG, 13)).showDialog(null)
        failure?.let { throw it }
        return result
    }

    private inline fun <reified T : Component> Container.named(name: String): T =
        descendants().filterIsInstance<T>().first { it.name == name }

    private fun Container.descendants(): List<Component> =
        components.flatMap { listOf(it) + ((it as? Container)?.descendants() ?: emptyList()) }

    companion object {
        @JvmStatic
        @BeforeAll
        fun loadMessages() {
            "/message/localization.*.prop".loadMessages()
        }
    }
}
