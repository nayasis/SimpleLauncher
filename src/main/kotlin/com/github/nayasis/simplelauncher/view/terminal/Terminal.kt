package com.github.nayasis.simplelauncher.view.terminal

import com.github.nayasis.kotlin.basica.core.extention.isNotEmpty
import com.github.nayasis.kotlin.basica.etc.Platforms
import com.github.nayasis.kotlin.basica.etc.error
import com.github.nayasis.kotlin.javafx.stage.Dialog
import javafx.application.Platform
import javafx.beans.property.SimpleObjectProperty
import javafx.event.EventHandler
import javafx.stage.Stage
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import mu.KotlinLogging
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.File
import java.io.IOException
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.io.Writer
import java.util.concurrent.LinkedBlockingQueue

private val logger = KotlinLogging.logger {}

class Terminal @JvmOverloads constructor(
    config: TerminalConfig = TerminalConfig().apply {
        copyOnSelect = true
        enableClipboardNotice = false
}): TerminalView(config) {

    private val outputWriterProperty = SimpleObjectProperty<Writer>()
    private val commandQueue = LinkedBlockingQueue<String>()

    private lateinit var command: Array<String>

    var stage: Stage? = null
        set(stage) {
            field = stage
            stage?.onCloseRequest = EventHandler {
                postAction = null
                close()
            }
        }

    var workingDirectory: String? = null
    var postAction: Runnable? = null
    var outputWriter: Writer
        get() = outputWriterProperty.get()
        set(writer) {
            outputWriterProperty.set(writer)
        }

    private var process: Process? = null

    fun setCommand(command: String, workingDirectory: String? = null): Terminal {
        this.command = command.split(" ").toTypedArray()
        if( workingDirectory.isNotEmpty() )
            this.workingDirectory = workingDirectory
        return this
    }

    @WebkitCall
    fun sendCommand(command: String) {
        commandQueue.put(command)
        try {
            val executableCommand = commandQueue.poll()
            outputWriter.write(executableCommand)
            outputWriter.flush()
        } catch (e: IOException) {
            logger.error(e)
        }
    }

    @WebkitCall
    override fun onTerminalReady() {
        GlobalScope.launch {
            try {
                runProcess()
                stage?.let {
                    Platform.runLater {
                        it.title = "${it.title} (done)"
                    }}
                postAction?.run()
            } catch (e: Exception) {
                logger.error(e)
                Platform.runLater { Dialog.error("msg.error.003", e) }
            }
        }
    }

    private fun runProcess() {
        try {
            process = ProcessBuilder(*command).apply {
                    if ( workingDirectory.isNotEmpty() ) directory(File(workingDirectory))
                }.start()
            inputReader  = BufferedReader(InputStreamReader(process?.inputStream, Platforms.os.charset))
            errorReader  = BufferedReader(InputStreamReader(process?.errorStream, Platforms.os.charset))
            outputWriter = BufferedWriter(OutputStreamWriter(process?.outputStream, Platforms.os.charset))
            focusCursor()
//            countDownLatch.countDown()
            process?.waitFor()
        } finally {
            closeStream()
        }
    }

    private fun closeStream() {
        try { inputReader.close() } catch (e: Exception) {}
        try { errorReader.close() } catch (e: Exception) {}
        try { outputWriter.close() } catch (e: Exception) {}
    }

    fun close() {
        try {
            process?.destroyForcibly()
            webView.engine.load(null)
        } finally {
            closeStream()
        }
    }

}
