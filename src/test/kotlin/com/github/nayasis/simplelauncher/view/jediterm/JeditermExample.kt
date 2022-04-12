package com.github.nayasis.simplelauncher.view.jediterm

import com.github.nayasis.kotlin.basica.exec.Command
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
        val process = ProcessBuilder("cmd.exe").apply {
//            redirectOutput(ProcessBuilder.Redirect.INHERIT)
//            redirectError(ProcessBuilder.Redirect.INHERIT)
//            redirectInput(ProcessBuilder.Redirect.INHERIT)
        }.start()
//    val process = Command("cmd.exe").run(null,null).process!!
//        val process = PtyProcessBuilder(arrayOf("cmd.exe")).start()

    return PureProcessTtyConnector(process, StandardCharsets.UTF_8)

}


private fun createAndShowGUI() {
    val frame = JFrame("Basic Terminal Example")
    frame.defaultCloseOperation = JFrame.EXIT_ON_CLOSE
    frame.contentPane = createTerminalWidget()
    frame.pack()
    frame.isVisible = true
}

