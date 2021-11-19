package com.github.nayasis.simplelauncher.view.terminal

import com.github.nayasis.kotlin.basica.etc.Platforms
import com.github.nayasis.kotlin.basica.exec.Command
import com.github.nayasis.kotlin.basica.exec.CommandExecutor
import mu.KotlinLogging
import tornadofx.runAsync
import java.io.BufferedReader
import java.io.InputStreamReader

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
): TerminalBasePane(config) {

    val executor = CommandExecutor().apply {
        onProcessFailed = { closeReader() }
    }

    override fun onTerminalReady() {
        runAsync {
            try {
                runProcess()
                runCatching{ onSuccess?.let{it()} }
            } catch (e: Throwable) {
                runCatching { onFail?.let { it(e) } }
            } finally {
                runCatching { closeReader() }
                runCatching { onDone?.let { it() } }
            }
        }
    }

    private fun runProcess() {
        executor.run(command,null,null)
        outputReader = BufferedReader(InputStreamReader(executor.outputStream, Platforms.os.charset))
        errorReader  = BufferedReader(InputStreamReader(executor.errorStream, Platforms.os.charset))
        focusCursor()
        executor.waitFor()
    }

    fun destory() {
        executor.destroy()
    }

}
