package com.github.nayasis.simplelauncher.view.terminal

import com.github.nayasis.kotlin.basica.core.extention.isNotEmpty
import com.github.nayasis.kotlin.basica.core.path.directory
import com.github.nayasis.kotlin.basica.core.string.toFile
import com.github.nayasis.kotlin.basica.etc.Platforms
import com.github.nayasis.kotlin.javafx.stage.Dialog
import com.github.nayasis.kotlin.javafx.stage.stage
import javafx.event.EventHandler
import javafx.stage.Stage
import mu.KotlinLogging
import org.apache.commons.exec.CommandLine
import org.apache.commons.exec.DefaultExecuteResultHandler
import org.apache.commons.exec.DefaultExecutor
import org.apache.commons.exec.ExecuteWatchdog
import org.apache.commons.exec.PumpStreamHandler
import tornadofx.runAsync
import tornadofx.runLater
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.PipedInputStream
import java.io.PipedOutputStream

/**
 * Terminal Pane
 *
 * https://github.com/javaterminal/TerminalFX
 */
class TerminalPane(
    val command: String,
    workingDirectory: String? = null,
    var postAction:(() -> Unit)? = null,
    config: TerminalConfig,
): TerminalBasePane(config) {

    val executor = DefaultExecutor().apply {
        if ( workingDirectory.isNotEmpty() )
            this.workingDirectory = workingDirectory!!.toFile().directory
        watchdog = ExecuteWatchdog(-1L)
    }

    override fun onTerminalReady() {
        runAsync {
            try {
                runProcess()
                scene?.stage?.let { runLater { it.title = "${it.title} (done)" }}
                postAction?.let { it() }
            } catch (e: Exception) {
                runLater { Dialog.error("msg.error.003", e) }
            } finally {
                runCatching { outputReader.close() }
            }
        }
    }

    private fun runProcess() {

        val resultHandler = DefaultExecuteResultHandler()

        val inStream = PipedInputStream()
        val outStream = PipedOutputStream(inStream)
        val pumpStream = PumpStreamHandler(outStream)

        outputReader = BufferedReader(InputStreamReader(inStream, Platforms.os.charset))

        executor.streamHandler = pumpStream
        executor.execute(CommandLine.parse(command), resultHandler)

        focusCursor()

        resultHandler.waitFor()

    }

}
