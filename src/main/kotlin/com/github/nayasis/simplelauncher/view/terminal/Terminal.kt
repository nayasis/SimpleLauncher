package com.github.nayasis.simplelauncher.view.terminal

import com.github.nayasis.kotlin.basica.core.extention.isNotEmpty
import com.github.nayasis.kotlin.basica.etc.Platforms
import com.github.nayasis.kotlin.javafx.stage.Dialog
import javafx.application.Platform
import javafx.event.EventHandler
import javafx.stage.Stage
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import mu.KotlinLogging
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader

private val logger = KotlinLogging.logger {}

class Terminal(
    config: TerminalConfig = TerminalConfig().apply {
        copyOnSelect = true
        enableClipboardNotice = false
}): TerminalView(config) {

    private lateinit var command: Array<String>

    var stage: Stage? = null
        set(stage) {
            field = stage
            stage?.onCloseRequest = EventHandler {
                postAction = null
                try {
                    process?.destroyForcibly()
                    webView.engine.load(null)
                } finally {
                    closeStream()
                }
            }
        }

    var workingDirectory: String? = null
    var postAction: Runnable? = null

    private var process: Process? = null

    fun setCommand(command: String, workingDirectory: String? = null): Terminal {
        this.command = command.split(" ").toTypedArray()
        if( workingDirectory.isNotEmpty() )
            this.workingDirectory = workingDirectory
        return this
    }

    override fun sendCommand(command: String) {}

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
                Platform.runLater { Dialog.error("msg.error.003", e) }
            }
        }
    }

    private fun runProcess() {
        try {
            process = ProcessBuilder(*command).apply {
                    if ( workingDirectory.isNotEmpty() ) directory(File(workingDirectory))
                }.start()
            outputReader = BufferedReader(InputStreamReader(process?.inputStream, Platforms.os.charset))
            errorReader = BufferedReader(InputStreamReader(process?.errorStream, Platforms.os.charset))
            focusCursor()
            process?.waitFor()
        } finally {
            closeStream()
        }
    }

    private fun closeStream() {
        try { outputReader.close() } catch (e: Exception) {}
        try { errorReader.close() } catch (e: Exception) {}
    }

}
