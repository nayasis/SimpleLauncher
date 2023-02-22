package com.github.nayasis.simplelauncher.view.terminalfximp

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty
import com.github.nayasis.simplelauncher.view.terminalfximp.helper.FxHelper
import javafx.scene.paint.Color
import java.util.*

class TerminalConfig {
    @JsonProperty("use-default-window-copy")
    var isUseDefaultWindowCopy = true

    @JsonProperty("clear-selection-after-copy")
    var isClearSelectionAfterCopy = true

    @JsonProperty("copy-on-select")
    var isCopyOnSelect = false

    @JsonProperty("ctrl-c-copy")
    var isCtrlCCopy = true

    @JsonProperty("ctrl-v-paste")
    var isCtrlVPaste = true

    @JsonProperty("cursor-color")
    var cursorColor = "black"

    @JsonProperty(value = "background-color")
    var backgroundColor = "white"

    @JsonProperty("font-size")
    var fontSize = 14

    @JsonProperty(value = "foreground-color")
    var foregroundColor = "black"

    @JsonProperty("cursor-blink")
    var isCursorBlink = false

    @JsonProperty("scrollbar-visible")
    var isScrollbarVisible = true

    @JsonProperty("enable-clipboard-notice")
    var isEnableClipboardNotice = true

    @JsonProperty("scroll-wheel-move-multiplier")
    var scrollWhellMoveMultiplier = 0.1

    @JsonProperty("font-family")
    var fontFamily = "\"DejaVu Sans Mono\", \"Everson Mono\", FreeMono, \"Menlo\", \"Terminal\", monospace"

    @JsonProperty(value = "user-css")
    var userCss = "data:text/plain;base64," + "eC1zY3JlZW4geyBjdXJzb3I6IGF1dG87IH0="

    @JsonIgnore
    var windowsTerminalStarter = "cmd.exe"

    @JsonIgnore
    var unixTerminalStarter = "/bin/bash -i"

    fun setBackgroundColor(color: Color) {
        backgroundColor = FxHelper.colorToHex(color)
    }

    fun setForegroundColor(color: Color) {
        foregroundColor = FxHelper.colorToHex(color)
    }

    fun setCursorColor(color: Color) {
        cursorColor = FxHelper.colorToHex(color)
    }

    override fun equals(o: Any?): Boolean {
        if (this === o) return true
        if (o == null || javaClass != o.javaClass) return false
        val that = o as TerminalConfig
        return isUseDefaultWindowCopy == that.isUseDefaultWindowCopy && isClearSelectionAfterCopy == that.isClearSelectionAfterCopy && isCopyOnSelect == that.isCopyOnSelect && isCtrlCCopy == that.isCtrlCCopy && isCtrlVPaste == that.isCtrlVPaste && fontSize == that.fontSize && isCursorBlink == that.isCursorBlink && isScrollbarVisible == that.isScrollbarVisible && isEnableClipboardNotice == that.isEnableClipboardNotice && java.lang.Double.compare(
            that.scrollWhellMoveMultiplier,
            scrollWhellMoveMultiplier
        ) == 0 && cursorColor == that.cursorColor && backgroundColor == that.backgroundColor && foregroundColor == that.foregroundColor && fontFamily == that.fontFamily && userCss == that.userCss && windowsTerminalStarter == that.windowsTerminalStarter && unixTerminalStarter == that.unixTerminalStarter
    }

    override fun hashCode(): Int {
        return Objects.hash(
            isUseDefaultWindowCopy,
            isClearSelectionAfterCopy,
            isCopyOnSelect,
            isCtrlCCopy,
            isCtrlVPaste,
            cursorColor,
            backgroundColor,
            fontSize,
            foregroundColor,
            isCursorBlink,
            isScrollbarVisible,
            isEnableClipboardNotice,
            scrollWhellMoveMultiplier,
            fontFamily,
            userCss,
            windowsTerminalStarter,
            unixTerminalStarter
        )
    }
}
