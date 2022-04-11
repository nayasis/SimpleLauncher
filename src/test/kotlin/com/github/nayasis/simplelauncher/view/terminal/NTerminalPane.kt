package com.github.nayasis.simplelauncher.view.terminal

import com.pty4j.PtyProcess
import com.pty4j.WinSize
import javafx.beans.property.SimpleObjectProperty
import mu.KotlinLogging
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.io.Writer
import java.util.concurrent.LinkedBlockingQueue
import kotlin.concurrent.thread

private val logger = KotlinLogging.logger {}

open class NTerminalPane(val process: Process): NTerminalView() {

    private val commandQueue = LinkedBlockingQueue<String>()

    private val outputWriterProperty = SimpleObjectProperty<Writer>()
    var outputWriter: Writer
        get() = outputWriterProperty.get()
        set(value) {
            outputWriterProperty.set(value)
        }

    @WebkitCall
    fun command(command: String?) {
        if(command == null) return
        commandQueue.put(command)
        thread() {
            println(">> polling command queue")
            commandQueue.poll().let { command ->
                outputWriterProperty.get().run {
                    write(command)
                    flush()
                }
            }
        }
    }

    override fun onTerminalReady() {
        thread() {
            logger.debug{">> on terminal ready !!"}
            initializeProcess()
        }
    }

    private fun initializeProcess() {

        columnsProperty.addListener { _, _, _ -> updateWinSize() }
        rowsProperty.addListener { _, _, _ -> updateWinSize() }

        updateWinSize()

        inputReader = BufferedReader(InputStreamReader(process.inputStream))
        errorReader = BufferedReader(InputStreamReader(process.errorStream))
        outputWriter = BufferedWriter(OutputStreamWriter(process.outputStream))

        focusCursor()

        countDownLatch.countDown()
        process.waitFor()

    }

    private fun updateWinSize() {
        (process as PtyProcess?)?.let {
            it.winSize = WinSize(column,row)
        }
    }

    fun close() {
        process.destroyForcibly()
        process.inputStream.close()
        process.errorStream.close()
        process.outputStream.close()
    }

}