package io.github.nayasis.simplelauncher.view.theme

import com.techsenger.jeditermfx.core.TerminalColor
import com.techsenger.jeditermfx.core.TextStyle
import com.techsenger.jeditermfx.ui.settings.DefaultSettingsProvider

class BlackTerminalTheme: DefaultSettingsProvider() {

    override fun getTerminalFontSize(): Float = 10.2f

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