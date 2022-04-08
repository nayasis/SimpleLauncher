package com.github.nayasis.simplelauncher.view.terminal

import com.github.nayasis.kotlin.basica.core.extention.ifNotEmpty
import com.github.nayasis.kotlin.basica.etc.Platforms
import com.github.nayasis.kotlin.basica.exec.Command
import com.pty4j.PtyProcess
import com.pty4j.PtyProcessBuilder
import com.pty4j.WinSize
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

    private lateinit var process: PtyProcess

    override fun onTerminalReady() {
        runAsync {
            try {
                initialize()
                runCatching{ onSuccess?.invoke() }
            } catch (e: Throwable) {
                runCatching { onFail?.invoke(e) }
            } finally {
                runCatching { close() }
                runCatching { onDone?.invoke() }
            }
        }
    }

    private fun initialize() {

        process      = toPtyProcess(command)
        inputReader  = BufferedReader(InputStreamReader(process.inputStream, Platforms.os.charset))
        errorReader  = BufferedReader(InputStreamReader(process.errorStream, Platforms.os.charset))
        outputWriter = BufferedWriter(OutputStreamWriter(process.outputStream, Platforms.os.charset))

        columnsProperty.addListener { _, _, _ -> updateWinSize() }
        rowsProperty.addListener { _, _, _ -> updateWinSize() }

        focusCursor()
        countDownLatch.countDown()

        process.waitFor()

    }

    private fun updateWinSize() {
        process.winSize = WinSize(column,row)
    }

    override fun close() {
        super.close()
        process.destroyForcibly()
        process.inputStream.close()
        process.errorStream.close()
        process.outputStream.close()
    }

    private fun toPtyProcess(command: Command): PtyProcess {
        return PtyProcessBuilder(command.command.toTypedArray()).apply{
            setEnvironment(command.environment)
            command.workingDirectory.ifNotEmpty { setDirectory(it) }
        }.start()
    }

}
