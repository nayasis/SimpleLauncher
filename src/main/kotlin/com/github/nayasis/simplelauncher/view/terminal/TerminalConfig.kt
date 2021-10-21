package com.github.nayasis.simplelauncher.view.terminal

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty

@JsonInclude(JsonInclude.Include.NON_NULL)
class TerminalConfig {

    @JsonProperty(value = "send-encoding")
    var sendEncoding = "raw"

    @JsonProperty(value = "receive-encoding")
    var receiveEncoding = "utf-8"

    @JsonProperty("use-default-window-copy")
    var useDefaultWindowCopy = true

    @JsonProperty("clear-selection-after-copy")
    var clearSelectionAfterCopy = true

    @JsonProperty("copy-on-select")
    var copyOnSelect = true

    @JsonProperty("ctrl-c-copy")
    var ctrlCCopy = true

    @JsonProperty("ctrl-v-paste")
    var ctrlVPaste = true

    @JsonProperty("cursor-color")
    var cursorColor = "black"

    @JsonProperty(value = "background-color")
    var backgroundColor = "white"

    @JsonProperty("font-size")
    var fontSize = 10

    @JsonProperty(value = "foreground-color")
    var foregroundColor = "black"

    @JsonProperty("cursor-blink")
    var cursorBlink = false

    @JsonProperty("scrollbar-visible")
    var scrollbarVisible = true

    @JsonProperty("enable-clipboard-notice")
    var enableClipboardNotice = false

    @JsonProperty("scroll-wheel-move-multiplier")
    var scrollWhellMoveMultiplier = 0.1

    @JsonProperty("font-family")
    var fontFamily = """
         "DejaVu Sans Mono", "Everson Mono", FreeMono, "Menlo", "Terminal", monospace
       """.trim()

    @JsonProperty(value = "user-css")
    var userCss = "data:text/plain;base64," + "eC1zY3JlZW4geyBjdXJzb3I6IGF1dG87IH0="

    @JsonIgnore
    var windowsTerminalStarter = "cmd.exe"

    @JsonIgnore
    var unixTerminalStarter = "/bin/bash -i"

    override fun equals(o: Any?): Boolean {
        if (this === o) return true
        if (o == null || javaClass != o.javaClass) return false
        val that = o as TerminalConfig
        if (useDefaultWindowCopy != o.useDefaultWindowCopy) return false
        if (clearSelectionAfterCopy != o.clearSelectionAfterCopy) return false
        if (copyOnSelect != o.copyOnSelect) return false
        if (ctrlCCopy != o.ctrlCCopy) return false
        if (ctrlVPaste != o.ctrlVPaste) return false
        if (fontSize != o.fontSize) return false
        if (cursorBlink != o.cursorBlink) return false
        if (scrollbarVisible != o.scrollbarVisible) return false
        if (enableClipboardNotice != o.enableClipboardNotice) return false
        if (java.lang.Double.compare(o.scrollWhellMoveMultiplier, scrollWhellMoveMultiplier) != 0) return false
        if (sendEncoding != o.sendEncoding) return false
        if (receiveEncoding != o.receiveEncoding) return false
        if (cursorColor != o.cursorColor) return false
        if (backgroundColor != o.backgroundColor) return false
        if (foregroundColor != o.foregroundColor) return false
        if (fontFamily != o.fontFamily) return false
        if (userCss != o.userCss) return false
        if (windowsTerminalStarter != o.windowsTerminalStarter) return false
        return unixTerminalStarter == o.unixTerminalStarter
    }

    override fun hashCode(): Int {
        var result: Int
        val temp: Long
        result = sendEncoding.hashCode()
        result = 31 * result + receiveEncoding.hashCode()
        result = 31 * result + if (useDefaultWindowCopy) 1 else 0
        result = 31 * result + if (clearSelectionAfterCopy) 1 else 0
        result = 31 * result + if (copyOnSelect) 1 else 0
        result = 31 * result + if (ctrlCCopy) 1 else 0
        result = 31 * result + if (ctrlVPaste) 1 else 0
        result = 31 * result + cursorColor.hashCode()
        result = 31 * result + backgroundColor.hashCode()
        result = 31 * result + fontSize
        result = 31 * result + foregroundColor.hashCode()
        result = 31 * result + if (cursorBlink) 1 else 0
        result = 31 * result + if (scrollbarVisible) 1 else 0
        result = 31 * result + if (enableClipboardNotice) 1 else 0
        temp = java.lang.Double.doubleToLongBits(scrollWhellMoveMultiplier)
        result = 31 * result + (temp xor (temp ushr 32)).toInt()
        result = 31 * result + fontFamily.hashCode()
        result = 31 * result + userCss.hashCode()
        result = 31 * result + windowsTerminalStarter.hashCode()
        result = 31 * result + unixTerminalStarter.hashCode()
        return result
    }

}