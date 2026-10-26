package io.github.nayasis.simplelauncher.view

import java.awt.Font
import java.awt.GraphicsEnvironment
import java.util.Locale
import javax.swing.UIManager
import javax.swing.plaf.FontUIResource
import javax.swing.text.StyleContext

/** The launcher's UI font; FlatLaf keeps this override when the theme changes. */
object AppFonts {

    const val MIN_SIZE = 8
    const val MAX_SIZE = 48

    val families: List<String> by lazy {
        GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames(Locale.ROOT)
            .sortedWith(String.CASE_INSENSITIVE_ORDER)
    }

    fun current(): Font = UIManager.getFont("Label.font") ?: Font(Font.DIALOG, Font.PLAIN, 12)

    /** StyleContext supplies glyph fallback for the languages in the preview and the UI. */
    fun font(family: String, size: Int): Font =
        StyleContext.getDefaultStyleContext().getFont(family, Font.PLAIN, size.coerceIn(MIN_SIZE, MAX_SIZE))

    fun install(family: String?, size: Int?) {
        val base = UIManager.getLookAndFeelDefaults().getFont("defaultFont") ?: current()
        val selected = if (family == null && size == null) null else FontUIResource(font(
            families.firstOrNull { it.equals(family, ignoreCase = true) } ?: base.family,
            size ?: base.size,
        ))
        UIManager.put("defaultFont", selected)
    }
}
