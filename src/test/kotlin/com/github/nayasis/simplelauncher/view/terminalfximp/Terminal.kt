package com.github.nayasis.simplelauncher.view.terminalfximp

import com.github.nayasis.kotlin.basica.etc.Platforms
import com.github.nayasis.simplelauncher.view.terminal.WebkitCall
import com.github.nayasis.simplelauncher.view.terminalfximp.helper.ThreadHelper
import com.pty4j.PtyProcess
import javafx.beans.property.ObjectProperty
import javafx.beans.property.SimpleObjectProperty
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.IOException
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.io.Writer
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.*
import java.util.concurrent.LinkedBlockingQueue

class Terminal(terminalConfig: TerminalConfig, terminalPath: Path? = null): TerminalView() {

    var process: Process? = null
        private set
    private val outputWriterProperty: ObjectProperty<Writer>
    val terminalPath: Path?

    private lateinit var termCommand: Array<String>
    private val commandQueue: LinkedBlockingQueue<String>

    init {
        this.terminalConfig = terminalConfig
        this.terminalPath = terminalPath
        outputWriterProperty = SimpleObjectProperty()
        commandQueue = LinkedBlockingQueue()
    }

    @WebkitCall
    fun command(command: String) {
        try {
            commandQueue.put(command)
        } catch (e: InterruptedException) {
            throw RuntimeException(e)
        }
        ThreadHelper.start {
            try {
                val commandToExecute = commandQueue.poll()
                outputWriter.write(commandToExecute)
                outputWriter.flush()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }

    @WebkitCall
    override fun onTerminalReady() {
        ThreadHelper.start {
            try {
                initializeProcess()
            } catch (e: Exception) {
            }
        }
    }

    @Throws(Exception::class)
    private fun initializeProcess() {
        val dataDir = dataDir
        if (Platforms.isWindows) {
            termCommand = terminalConfig.windowsTerminalStarter.split("\\s+").toTypedArray()
        } else {
            termCommand = terminalConfig.unixTerminalStarter.split("\\s+").toTypedArray()
        }
        val envs: MutableMap<String, String> = HashMap(System.getenv())
        envs["TERM"] = "xterm"

        System.setProperty("PTY_LIB_FOLDER", dataDir.resolve("libpty").toString())
        if (Objects.nonNull(terminalPath) && Files.exists(terminalPath)) {
            process = PtyProcess.exec(termCommand, envs, terminalPath.toString())
        } else {
            process = PtyProcess.exec(termCommand, envs)
        }
        columnsProperty().addListener { evt -> updateWinSize() }
        rowsProperty().addListener { evt -> updateWinSize() }
        updateWinSize()
        val defaultCharEncoding = System.getProperty("file.encoding")

        inputReader  = BufferedReader(InputStreamReader(process!!.inputStream, defaultCharEncoding))
        errorReader  = BufferedReader(InputStreamReader(process!!.errorStream, defaultCharEncoding))
        outputWriter = BufferedWriter(OutputStreamWriter(process!!.outputStream, defaultCharEncoding))
        focusCursor()
        countDownLatch.countDown()
        process!!.waitFor()
    }

    private val dataDir: Path
        private get() {
            val userHome = System.getProperty("user.home")
            return Paths.get(userHome).resolve(".terminalfx")
        }

    private fun updateWinSize() {
        try {
//            process!!.setWinSize(WinSize(getColumns(), getRows()))
        } catch (e: Exception) {
            //
        }
    }

    fun outputWriterProperty(): ObjectProperty<Writer> {
        return outputWriterProperty
    }

    var outputWriter: Writer
        get() = outputWriterProperty.get()
        set(writer) {
            outputWriterProperty.set(writer)
        }
}
