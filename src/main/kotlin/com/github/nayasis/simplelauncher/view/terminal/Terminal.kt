package com.github.nayasis.simplelauncher.view.terminal

import com.github.nayasis.kotlin.basica.core.extention.isNotEmpty
import com.github.nayasis.kotlin.basica.etc.Platforms
import com.github.nayasis.kotlin.basica.etc.error
import com.github.nayasis.kotlin.javafx.stage.Dialog
import javafx.application.Platform
import javafx.beans.property.ObjectProperty
import javafx.beans.property.SimpleObjectProperty
import javafx.event.EventHandler
import javafx.stage.Stage
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

class Terminal @JvmOverloads constructor(terminalConfig: TerminalConfig?): TerminalView() {

    private val outputWriterProperty: ObjectProperty<Writer> = SimpleObjectProperty()
    private val commandQueue = LinkedBlockingQueue<String>()
    private lateinit var command: Array<String>

    var stage: Stage? = null
        private set

    var workingDirectory: String? = null
    var postAction: Runnable? = null

    private var process: Process? = null

    fun setCommand(command: String, workingDirectory: String? = null): Terminal {
        this.command = command.split(" ").toTypedArray()
        if( workingDirectory.isNotEmpty() )
            this.workingDirectory = workingDirectory
        return this
    }

    fun setStage(stage: Stage?): Terminal {
        this.stage = stage
        this.stage!!.onCloseRequest = EventHandler {
            postAction = null
            close()
        }
        return this
    }

    @WebkitCall
    fun sendCommand(command: String) {
        try {
            commandQueue.put(command)
        } catch (e: InterruptedException) {
            throw RuntimeException(e)
        }
        run {
            try {
                val commandToExecute = commandQueue.poll()
                outputWriter.write(commandToExecute)
                outputWriter.flush()
            } catch (e: IOException) {
                logger.error(e)
            }
        }
    }

    @WebkitCall
    override fun onTerminalReady() {
        run {
            try {
                runProcess()
                if (stage != null) {
                    Platform.runLater {
                        stage!!.title = "${stage!!.title} (done)"
                    }
                }
                if (postAction != null) {
                    postAction!!.run()
                }
            } catch (e: Exception) {
                logger.error(e)
                Platform.runLater { Dialog.error("msg.error.003", e) }
            }
        }
    }

    @Throws(Exception::class)
    private fun runProcess() {
        val builder = ProcessBuilder(*command)
        if ( ! workingDirectory.isNullOrEmpty() ) {
            builder.directory(File(workingDirectory))
        }
        process = builder.start()
        inputReader = BufferedReader(InputStreamReader(process?.inputStream, Platforms.os.charset))
        errorReader = BufferedReader(InputStreamReader(process?.errorStream, Platforms.os.charset))
        outputWriter = BufferedWriter(OutputStreamWriter(process?.outputStream, Platforms.os.charset))
        focusCursor()
        countDownLatch.countDown()
        process?.waitFor()
        closeStream()
    }

    private fun closeStream() {
        try {
            inputReader.close()
        } catch (e: Exception) {}
        try {
            errorReader.close()
        } catch (e: Exception) {}
        try {
            outputWriter.close()
        } catch (e: Exception) {}
    }

    fun outputWriterProperty(): ObjectProperty<Writer> {
        return outputWriterProperty
    }

    var outputWriter: Writer
        get() = outputWriterProperty.get()
        set(writer) {
            outputWriterProperty.set(writer)
        }

    fun close() {
        if (process != null) {
            process!!.destroyForcibly()
        }
        closeStream()
        webView.engine.load(null)
    }

    init {
        if (terminalConfig == null) {
            this.terminalConfig = TerminalConfig().apply {
                copyOnSelect = true
                enableClipboardNotice = false
            }
        }
    }

}
