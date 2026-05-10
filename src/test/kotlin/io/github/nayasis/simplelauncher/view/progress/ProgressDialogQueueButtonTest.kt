package io.github.nayasis.simplelauncher.view.progress

import io.github.nayasis.kotlin.basica.exec.Command
import io.github.nayasis.kotlin.javafx.stage.Dialog
import io.github.nayasis.simplelauncher.service.ProgressFileQueue
import io.github.nayasis.simplelauncher.service.ProgressQueueControl
import io.github.nayasis.simplelauncher.service.ProgressQueuePopOver
import javafx.scene.Scene
import javafx.scene.control.Button
import javafx.scene.control.Label
import javafx.scene.control.Tooltip
import javafx.scene.layout.VBox
import javafx.stage.Stage
import tornadofx.action
import tornadofx.button
import tornadofx.label
import tornadofx.launch
import tornadofx.runLater
import java.io.OutputStream
import java.io.PrintStream
import java.io.File

fun main(args: Array<String>) {
    System.setProperty("javafx.suppressUnsupportedConfiguration", "true")
    suppressJavaFxTestWarnings()
    launch<ProgressDialogQueueButtonTest>(args)
}

class ProgressDialogQueueButtonTest: tornadofx.App() {

    private lateinit var status: Label

    override fun start(stage: Stage) {
        status = Label("Idle")
        stage.title = "ProgressDialog queue button test"
        stage.scene = Scene(
            VBox(8.0).apply {
                prefWidth = 360.0
                prefHeight = 120.0
                style = "-fx-padding: 12;"
                button("Add 5 dummy files") {
                    action { openProgressQueue() }
                }
                status = label("Idle")
            }
        )
        stage.show()
    }

    private fun openProgressQueue() {
        val queue = ProgressFileQueue(
            (1..5).map { index -> File("D:/dummy/add-game/game-$index.webp") }
        )

        lateinit var popOver: ProgressQueuePopOver
        val control = ProgressQueueControl(queue) {
            popOver.refreshIfShowing()
        }
        val queueButton = Button("☰").apply {
            tooltip = Tooltip("Work queue")
            style = "-fx-padding: 0 4 0 4; -fx-min-width: 20px; -fx-pref-width: 20px; -fx-max-width: 20px; -fx-min-height: 18px; -fx-pref-height: 18px; -fx-max-height: 18px; -fx-font-size: 10px;"
            setOnAction { event ->
                event.consume()
                popOver.toggle(this)
            }
        }
        lateinit var dialog: io.github.nayasis.kotlin.javafx.stage.progress.ProgressDialog
        popOver = ProgressQueuePopOver(
            items = queue,
            control = control,
            onPendingRemoved = {
                dialog.updateProgress(queue.currNo, queue.size.coerceAtLeast(1))
                dialog.updateSubMessage("(${queue.currNo}/${queue.size})")
            },
        )

        status.text = "Running"
        dialog = Dialog.progress("게임추가", headerButton = queueButton) { progress ->
            while (queue.hasNext()) {
                val item = queue.next() ?: continue
                progress.updateMessage(item.title)
                progress.updateProgress(queue.currNo, queue.size)
                progress.updateSubMessage("(${queue.currNo}/${queue.size})")

                val executor = Command("powershell -NoProfile -Command \"Start-Sleep -Seconds 10\"").run()
                control.updateExecutor(executor)
                try {
                    for (second in 1..10) {
                        if (!executor.isAlive) break
                        progress.updateSubMessage("(${queue.currNo}/${queue.size}) $second/10 sec")
                        Thread.sleep(1_000)
                    }
                } finally {
                    executor.destroy()
                    control.updateExecutor(null)
                    queue.finish(item)
                    popOver.refreshIfShowing()
                }
            }
            runLater {
                status.text = "Done"
            }
        }.setOnDone {
            popOver.hide()
        }
    }
}

private fun suppressJavaFxTestWarnings() {
    System.setErr(PrintStream(JavaFxWarningFilter(System.err), true, Charsets.UTF_8))
}

private class JavaFxWarningFilter(
    private val delegate: PrintStream,
): OutputStream() {

    private val buffer = StringBuilder()
    private var suppressNextUnsupportedConfigWarning = false

    override fun write(b: Int) {
        val char = b.toChar()
        if (char == '\n') {
            flushLine()
        } else if (char != '\r') {
            buffer.append(char)
        }
    }

    override fun flush() {
        if (buffer.isNotEmpty()) {
            flushLine()
        }
        delegate.flush()
    }

    private fun flushLine() {
        val line = buffer.toString()
        buffer.clear()
        when {
            line.contains("com.sun.javafx.application.PlatformImpl startup") -> {
                suppressNextUnsupportedConfigWarning = true
            }
            suppressNextUnsupportedConfigWarning && line.contains("Unsupported JavaFX configuration") -> {
                suppressNextUnsupportedConfigWarning = false
            }
            isNativeAccessWarning(line) -> Unit
            else -> delegate.println(line)
        }
    }

    private fun isNativeAccessWarning(line: String): Boolean {
        return line.contains("A restricted method in java.lang.System has been called")
            || line.contains("java.lang.System::load has been called by com.sun.glass.utils.NativeLibLoader")
            || line.contains("Use --enable-native-access=ALL-UNNAMED")
            || line.contains("Restricted methods will be blocked in a future release")
    }
}
