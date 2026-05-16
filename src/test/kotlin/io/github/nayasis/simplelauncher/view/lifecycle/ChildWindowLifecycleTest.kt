package io.github.nayasis.simplelauncher.view.lifecycle

import io.github.nayasis.kotlin.basica.exec.Command
import io.github.nayasis.kotlin.javafx.stage.Dialog
import io.github.nayasis.simplelauncher.service.LauncherChildWindows
import io.github.nayasis.simplelauncher.view.Terminal
import javafx.application.Application
import javafx.geometry.Insets
import javafx.scene.Scene
import javafx.scene.control.Label
import javafx.scene.layout.VBox
import javafx.stage.Stage
import javafx.stage.WindowEvent
import tornadofx.action
import tornadofx.button
import tornadofx.label

fun main(args: Array<String>) {
    System.setProperty("javafx.suppressUnsupportedConfiguration", "true")
    Application.launch(ChildWindowLifecycleTest::class.java, *args)
}

class ChildWindowLifecycleTest: Application() {

    private val childWindows = LauncherChildWindows()
    private lateinit var status: Label

    override fun start(stage: Stage) {
        stage.addEventHandler(WindowEvent.WINDOW_HIDDEN) {
            childWindows.hide()
        }
        stage.addEventHandler(WindowEvent.WINDOW_SHOWN) {
            childWindows.restore()
        }
        stage.iconifiedProperty().addListener { _, _, iconified ->
            when(iconified) {
                true -> childWindows.hide()
                else -> childWindows.restore()
            }
        }

        stage.title = "Child window lifecycle test"
        stage.scene = Scene(
            VBox(8.0).apply {
                padding = Insets(12.0)
                prefWidth = 360.0
                button("Open progress and terminal") {
                    action { openChildren() }
                }
                status = label("Idle")
            }
        )
        stage.show()
    }

    private fun openChildren() {
        openProgress()
        openTerminal()
        status.text = "Progress and terminal opened"
    }

    private fun openProgress() {
        lateinit var dialog: io.github.nayasis.kotlin.javafx.stage.progress.ProgressDialog
        dialog = Dialog.progress("Manual progress") { progress ->
            val totalSeconds = 10 * 60
            for (second in 1..totalSeconds) {
                progress.updateMessage("long-running progress")
                progress.updateProgress(second.toLong(), totalSeconds.toLong())
                progress.updateSubMessage("$second/$totalSeconds sec")
                Thread.sleep(1_000)
            }
        }.setOnDone {
            childWindows.unregister(dialog)
        }
        childWindows.register(dialog)
        status.text = "Progress opened"
    }

    private fun openTerminal() {
        val terminal = Terminal()
        childWindows.register(terminal)
        childWindows.show(terminal)
        terminal.run(Command("powershell -NoProfile -Command \"Write-Host manual-terminal\""))
        status.text = "Terminal opened"
    }

}
