package com.github.nayasis.simplelauncher.view.jediterm

import com.jediterm.terminal.emulator.ColorPalette
import com.jediterm.terminal.emulator.ColorPaletteImpl
import com.jediterm.terminal.ui.TerminalActionPresentation
import com.jediterm.terminal.ui.settings.DefaultSettingsProvider
import java.awt.Font
import java.awt.event.InputEvent
import java.awt.event.KeyEvent.*
import javax.swing.KeyStroke

class SettingProvider: DefaultSettingsProvider() {

    override fun getTerminalFont(): Font {
        return Font("나눔고딕코딩", Font.PLAIN, terminalFontSize.toInt())
    }

    override fun getTerminalFontSize(): Float {
        return 13.0F
    }

    override fun emulateX11CopyPaste(): Boolean = true

    override fun getPasteActionPresentation(): TerminalActionPresentation {
        val keyStroke = KeyStroke.getKeyStroke(VK_INSERT, InputEvent.SHIFT_DOWN_MASK)
        return TerminalActionPresentation("Paste", keyStroke)
    }

    override fun getTerminalColorPalette(): ColorPalette {
        return ColorPaletteImpl.XTERM_PALETTE
    }

}