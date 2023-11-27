package com.github.nayasis.swing

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.swing.Swing
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.Insets
import java.util.concurrent.CompletableFuture
import javax.swing.JFrame
import javax.swing.JPanel
import javax.swing.JProgressBar
import javax.swing.JScrollPane
import javax.swing.JTextArea
import javax.swing.SwingUtilities

fun main() {
    SwingUtilities.invokeLater { SwingDemo().initialize() }
}

class SwingDemo {
    fun initialize() {
        val progressBar = JProgressBar(0,100).apply {
            value = 0
            isStringPainted = true
        }

        val textArea = JTextArea(11,10).apply {
            margin = Insets(5,5,5,5)
            isEditable = true
            text = "Hello kotlin area ~\n"
        }

        val panel = JPanel().apply {
            add(progressBar)
            add(textArea)
        }

//    val scrollPane = JScrollPane(textArea)
        val frame = JFrame("Hello world").apply {
            contentPane.add(panel, BorderLayout.CENTER)
            defaultCloseOperation = JFrame.EXIT_ON_CLOSE
            size = Dimension(600,400)
            setLocationRelativeTo(null)
            pack()
            isVisible = true
        }

        GlobalScope.launch (Dispatchers.Swing) {
            for(i in 1..10) {
                textArea.append(startLongAsyncOperation(i))
                progressBar.value = i * 10
            }
        }

        println(">> done initialized")

    }

    private suspend fun startLongAsyncOperation(v: Int): String {
        delay(1000)
        return "Message: $v\n"
    }

}





//fun main(args: Array<String>) {
//    val textArea = JTextArea()
//    textArea.text = "Hello, SnowDeer"
//    val scrollPane = JScrollPane(textArea)
//
//    val frame = JFrame("Hello, SnowDeer")
//    frame.contentPane.add(scrollPane, BorderLayout.CENTER)
//    frame.defaultCloseOperation = JFrame.EXIT_ON_CLOSE
//    frame.size = Dimension(600, 400)
//    frame.setLocationRelativeTo(null)
//    frame.isVisible = true
//}