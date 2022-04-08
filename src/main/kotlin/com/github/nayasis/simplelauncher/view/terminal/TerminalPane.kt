package com.github.nayasis.simplelauncher.view.terminal

import com.github.nayasis.kotlin.basica.etc.Platforms
import com.github.nayasis.kotlin.basica.exec.Command
import com.github.nayasis.simplelauncher.common.runPty
import com.pty4j.PtyProcess
import com.pty4j.WinSize
import mu.KotlinLogging
import tornadofx.*
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
//                runCatching { close() }
                runCatching { onDone?.invoke() }
            }
        }
    }

    private fun initialize() {

        process      = command.runPty()
        inputReader  = BufferedReader(InputStreamReader(process.inputStream, Charsets.UTF_8))
        errorReader  = BufferedReader(InputStreamReader(process.errorStream, Charsets.UTF_8))
        outputWriter = BufferedWriter(OutputStreamWriter(process.outputStream, Charsets.UTF_8))

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

}
