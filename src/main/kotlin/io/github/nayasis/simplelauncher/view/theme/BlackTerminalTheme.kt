package io.github.nayasis.simplelauncher.view.theme

import com.jediterm.terminal.TerminalColor
import com.jediterm.terminal.TextStyle
import com.jediterm.terminal.ui.settings.DefaultSettingsProvider
import java.awt.Font
import javax.swing.text.StyleContext

class BlackTerminalTheme: DefaultSettingsProvider() {

    override fun getTerminalFontSize(): Float = 10.2f

    // a plain physical font has no glyph fallback, so Hangul would render as boxes.
    // StyleContext returns a composite font that falls back to a system font for missing glyphs.
    override fun getTerminalFont(): Font {
        val base = super.getTerminalFont()
        return StyleContext.getDefaultStyleContext().getFont(base.family, Font.PLAIN, base.size)
    }

    override fun getLineSpacing(): Float = 1.2f

    // Basic color
    override fun getDefaultForeground(): TerminalColor {
        return TerminalColor.rgb(240, 240, 240) // Brighter white
    }

    override fun getDefaultBackground(): TerminalColor {
        return TerminalColor.rgb(20, 20, 20) // Darker black
    }

    // Set cursor color to be brighter
    @Suppress("OVERRIDE_DEPRECATION")
    override fun getDefaultStyle(): TextStyle {
        return TextStyle(
            TerminalColor.rgb(240, 240, 240), // Foreground: Bright white
            TerminalColor.rgb(20, 20, 20)     // Background: Dark black
        )
    }

    // Selection area
    override fun getSelectionColor(): TextStyle {
        return TextStyle(
            TerminalColor.rgb(255, 255, 255), // Foreground: White (selected text)
            TerminalColor.rgb(0, 120, 215)    // Background: Blue (Windows style)
        )
    }

    // Found pattern
    override fun getFoundPatternColor(): TextStyle {
        return TextStyle(
            TerminalColor.rgb(255, 255, 255), // Foreground: White
            TerminalColor.rgb(255, 200, 0)    // Background: Orange
        )
    }

    // Hyperlink
    override fun getHyperlinkColor(): TextStyle {
        return TextStyle(
            TerminalColor.rgb(100, 200, 255), // Foreground: Bright blue
            TerminalColor.rgb(20, 20, 20)     // Background: Dark black
        )
    }

    // Use inverse selection color for better visibility of selected text
    override fun useInverseSelectionColor(): Boolean = true

}