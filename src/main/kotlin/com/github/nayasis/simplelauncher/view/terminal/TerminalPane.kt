package com.github.nayasis.simplelauncher.view.terminal

import com.github.nayasis.kotlin.basica.core.extention.ifNotEmpty
import com.github.nayasis.kotlin.basica.core.string.toFile
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
): TerminalBasePane(config) {

//    var executor: CommandExecutor? = null

    lateinit var process: Process

    override fun onTerminalReady() {
        runAsync {
            try {
                runProcess()
                runCatching{ onSuccess?.invoke() }
            } catch (e: Throwable) {
                runCatching { onFail?.invoke(e) }
            } finally {
                runCatching { onDone?.invoke() }
            }
        }
    }

    private fun runProcess() {
//        executor = command.run(redirect = false,{},{})
//        executor = command.run({},{})
//        outputReader = BufferedReader(InputStreamReader(executor!!.output, Platforms.os.charset))
//        inputWriter  = BufferedWriter(OutputStreamWriter(executor!!.input, Platforms.os.charset))
//        focusCursor()
//        executor!!.waitFor()

        process = run(command)
        outputReader = BufferedReader(InputStreamReader(process.inputStream, Platforms.os.charset))
        inputWriter  = BufferedWriter(OutputStreamWriter(process.outputStream, Platforms.os.charset))

        focusCursor()

//        process.waitFor()

    }

    fun close() {
//        executor?.destroy()
        super.closeReader()
    }

    private fun run(command: Command): Process {
        return ProcessBuilder(command.command).apply {
            environment().putAll(command.environment)
            command.workingDirectory?.toFile().ifNotEmpty { if (it.exists()) directory(it) }
        }.start()
    }

}
