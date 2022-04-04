package com.github.nayasis.simplelauncher.view.jediterm

import com.jediterm.pty.PtyProcessTtyConnector
import com.jediterm.terminal.TtyConnector
import com.jediterm.terminal.ui.JediTermWidget
import com.jediterm.terminal.ui.UIUtil
import com.pty4j.PtyProcess
import com.pty4j.PtyProcessBuilder
import java.awt.event.ComponentAdapter
import java.awt.event.ComponentEvent
import java.nio.charset.StandardCharsets
import javax.swing.JFrame
import javax.swing.SwingUtilities
import javax.swing.UIManager


fun main(vararg args: String) {
    runCatching {
        UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName())
    }
    SwingUtilities.invokeLater(::createAndShowGUI);
}

fun createTerminalWidget(): JediTermWidget {
//    return JediTermWidget(80, 24, SettingProvider()).apply {
//        ttyConnector = createTtyConnector()
//        start()
//    }
    return CustomTerminal(120, 40, SettingProvider()).apply {
        val self = this
        ttyConnector = createTtyConnector()
        addComponentListener(object: ComponentAdapter() {
            override fun componentResized(e: ComponentEvent?) {
                println( "- ${self.terminalPanel.boundedRangeModel}" )
            }
        })
        start()
    }
}

private fun createTtyConnector(): TtyConnector? {
    return try {
        var envs = System.getenv()
        val command: Array<String>
        if (UIUtil.isWindows) {
            command = arrayOf("cmd.exe")
        } else {
            command = arrayOf("/bin/bash", "--login")
            envs = HashMap(System.getenv())
            envs["TERM"] = "xterm-256color"
        }
        val process: PtyProcess = PtyProcessBuilder().setCommand(command).setEnvironment(envs).start()
        PtyProcessTtyConnector(process, StandardCharsets.UTF_8)
    } catch (e: Exception) {
        throw IllegalStateException(e)
    }
}


private fun createAndShowGUI() {
    val frame = JFrame("Basic Terminal Example")
    frame.defaultCloseOperation = JFrame.EXIT_ON_CLOSE
    frame.contentPane = createTerminalWidget()
    frame.pack()
    frame.isVisible = true
}

