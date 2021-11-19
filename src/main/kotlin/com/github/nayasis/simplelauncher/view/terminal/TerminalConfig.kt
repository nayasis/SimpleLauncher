package com.github.nayasis.simplelauncher.view.terminal

import au.com.console.kassava.kotlinEquals
import au.com.console.kassava.kotlinHashCode
import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import kotlin.reflect.full.memberProperties

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
    var userCss = "data:text/plain;base64,eC1zY3JlZW4geyBjdXJzb3I6IGF1dG87IH0="

    @JsonIgnore
    var windowsTerminalStarter = "cmd.exe"

    @JsonIgnore
    var unixTerminalStarter = "/bin/bash -i"

    override fun equals(other: Any?) = kotlinEquals(other,TerminalConfig::class.memberProperties.toTypedArray())

    override fun hashCode(): Int = kotlinHashCode(properties = TerminalConfig::class.memberProperties.toTypedArray())

}