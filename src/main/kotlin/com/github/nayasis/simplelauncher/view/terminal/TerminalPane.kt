package com.github.nayasis.simplelauncher.view.terminal

import com.github.nayasis.kotlin.basica.core.string.message
import com.github.nayasis.kotlin.basica.etc.Platforms
import com.github.nayasis.kotlin.basica.exec.Command
import com.github.nayasis.kotlin.basica.exec.CommandExecutor
import com.github.nayasis.kotlin.javafx.stage.Dialog
import com.github.nayasis.kotlin.javafx.stage.stage
import mu.KotlinLogging
import tornadofx.runAsync
import tornadofx.runLater
import java.io.BufferedReader
import java.io.InputStreamReader

private val logger = KotlinLogging.logger {}

/**
 * Terminal Pane
 *
 * https://github.com/javaterminal/TerminalFX
 */
class TerminalPane(
    val command: String,
    workingDirectory: String? = null,
    var onDone:(() -> Unit)? = null,
    config: TerminalConfig,
): TerminalBasePane(config) {

    val cmd = Command(command,workingDirectory)

    val executor = CommandExecutor().apply {
        onProcessFailed = { e ->
            runLater { Dialog.error(e) }
            closeReader()
        }
    }

    override fun onTerminalReady() {
        runAsync {
            try {
                runProcess()
                onDone?.let { it() }
            } catch (e: Exception) {
                runLater { Dialog.error("msg.error.003".message(), e) }
            } finally {
                runCatching { closeReader() }
            }
        }
    }

    private fun runProcess() {
        executor.run(cmd)
        outputReader = BufferedReader(InputStreamReader(executor.outputStream, Platforms.os.charset))
        errorReader  = BufferedReader(InputStreamReader(executor.errorStream, Platforms.os.charset))
        focusCursor()
        executor.waitFor()
    }

    fun destory() {
        executor.destroy()
    }

}
