package com.github.nayasis.simplelauncher.view.terminal

import javafx.scene.paint.Color

interface TerminalIf {

    @WebkitCall
    fun sendCommand(command: String)

    @WebkitCall
    fun getPrefs(): String

    @WebkitCall
    fun updatePrefs(config: TerminalConfig)

    @WebkitCall
    fun resizeTerminal(columns: Int, rows: Int)

    @WebkitCall
    fun onTerminalInit()

    @WebkitCall
    fun onTerminalReady()

    @WebkitCall
    fun copy(text: String)

}

annotation class WebkitCall(val from: String = "")

fun Color.toHex(): String {
    return "#%02X%02X%02X".format(
        (red * 255).toInt(),
        (green * 255).toInt(),
        (blue * 255).toInt()
    )
}