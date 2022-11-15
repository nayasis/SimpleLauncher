package com.github.nayasis.simplelauncher.view.terminal

import com.github.nayasis.kotlin.basica.etc.Platforms
import com.github.nayasis.kotlin.basica.exec.Command
import mu.KotlinLogging
import tornadofx.runAsync
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.InputStreamReader
import java.io.OutputStreamWriter

private val logger = KotlinLogging.logger {}

/**
 * Terminal Pane
 *
 * https://github.com/javaterminal/TerminalFX
 */
class TerminalPane(
    val command: Command,
    var onDone:(() -> Unit)?,
    var onFail: ((Throwable) -> Unit)?,
    var onSuccess: (() -> Unit)?,
    config: TerminalConfig,
): TerminalView(config) {

    private lateinit var process: Process

    override fun onTerminalReady() {
        runAsync {
            try {
                runProcess()
                runCatching{ onSuccess?.let{it()} }
            } catch (e: Throwable) {
                runCatching { onFail?.let { it(e) } }
            } finally {
                runCatching { close() }
                runCatching { onDone?.let { it() } }
            }
        }
    }

    private fun runProcess() {
        process = command.runProcess()
        outputReader = BufferedReader(InputStreamReader(process.inputStream, Platforms.os.charset))
        inputWriter  = BufferedWriter(OutputStreamWriter(process.outputStream, Platforms.os.charset))
        focusCursor()
        process.waitFor()
    }

    fun close() {
        process.destroy()
        super.closeReader()
    }

}
