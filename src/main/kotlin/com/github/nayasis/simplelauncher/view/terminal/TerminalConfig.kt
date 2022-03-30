package com.github.nayasis.simplelauncher.view.terminal

import au.com.console.kassava.kotlinEquals
import au.com.console.kassava.kotlinHashCode
import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import kotlin.reflect.full.memberProperties

@JsonInclude(JsonInclude.Include.NON_NULL)
class TerminalConfig {

    @field:JsonProperty(value = "send-encoding")
    var sendEncoding = "raw"

    @field:JsonProperty(value = "receive-encoding")
    var receiveEncoding = "utf-8"

    @field:JsonProperty("use-default-window-copy")
    var useDefaultWindowCopy = true

    @field:JsonProperty("clear-selection-after-copy")
    var clearSelectionAfterCopy = true

    @field:JsonProperty("copy-on-select")
    var copyOnSelect = true

    @field:JsonProperty("ctrl-c-copy")
    var ctrlCCopy = true

    @field:JsonProperty("ctrl-v-paste")
    var ctrlVPaste = true

    @field:JsonProperty("cursor-color")
    var cursorColor = "white"

    @field:JsonProperty(value = "background-color")
    var backgroundColor = "white"

    @field:JsonProperty("font-size")
    var fontSize = 10

    @field:JsonProperty(value = "foreground-color")
    var foregroundColor = "black"

    @field:JsonProperty("cursor-blink")
    var cursorBlink = false

    @field:JsonProperty("scrollbar-visible")
    var scrollbarVisible = true

    @field:JsonProperty("enable-clipboard-notice")
    var enableClipboardNotice = false

    @field:JsonProperty("scroll-wheel-move-multiplier")
    var scrollWhellMoveMultiplier = 0.1

    @field:JsonProperty("font-family")
    var fontFamily = """
         "DejaVu Sans Mono", "Everson Mono", FreeMono, "Menlo", "Terminal", monospace
       """.trim()

    @field:JsonProperty(value = "user-css")
    var userCss = "data:text/plain;base64,eC1zY3JlZW4geyBjdXJzb3I6IGF1dG87IH0="

    @JsonIgnore
    var windowsTerminalStarter = "cmd.exe"

    @JsonIgnore
    var unixTerminalStarter = "/bin/bash -i"

    override fun equals(other: Any?) = kotlinEquals(other,TerminalConfig::class.memberProperties.toTypedArray())

    override fun hashCode(): Int = kotlinHashCode(properties = TerminalConfig::class.memberProperties.toTypedArray())

}