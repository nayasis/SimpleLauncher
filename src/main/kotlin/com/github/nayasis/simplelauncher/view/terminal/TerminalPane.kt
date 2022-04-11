package com.github.nayasis.simplelauncher.view.terminal

import com.github.nayasis.kotlin.basica.etc.Platforms
import com.github.nayasis.kotlin.basica.exec.Command
import com.github.nayasis.kotlin.basica.exec.CommandExecutor
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
    var onDone:(() -> Unit)? = null,
    var onFail: ((Throwable) -> Unit)? = null,
    var onSuccess: (() -> Unit)? = null,
    config: TerminalConfig,
): TerminalView(config) {

    private lateinit var executor: CommandExecutor

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
        executor = command.runProcess(false)
        outputReader = BufferedReader(InputStreamReader(executor.output, Platforms.os.charset))
        errorReader  = BufferedReader(InputStreamReader(executor.error, Platforms.os.charset))
        inputWriter  = BufferedWriter(OutputStreamWriter(executor.input, Platforms.os.charset))
        focusCursor()
        executor.waitFor()
    }

    fun close() {
        executor.destroy()
        super.closeReader()
    }

}
