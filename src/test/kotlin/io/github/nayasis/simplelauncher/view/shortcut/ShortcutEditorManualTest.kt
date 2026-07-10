package io.github.nayasis.simplelauncher.view.shortcut

import io.github.nayasis.kotlin.basica.model.Messages.Companion.loadMessages
import io.github.nayasis.simplelauncher.service.ShortcutSettings
import io.github.nayasis.simplelauncher.view.ShortcutEditor
import javafx.application.Application
import javafx.application.Platform
import javafx.scene.Scene
import javafx.scene.layout.StackPane
import javafx.stage.Stage

fun main(args: Array<String>) {
    System.setProperty("javafx.suppressUnsupportedConfiguration", "true")
    "/message/**.prop".loadMessages()
    Application.launch(ShortcutEditorManualTest::class.java, *args)
}

class ShortcutEditorManualTest: Application() {

    override fun start(stage: Stage) {
        stage.title   = "Shortcut editor manual test"
        stage.scene   = Scene(StackPane(), 1.0, 1.0)
        stage.width   = 1.0
        stage.height  = 1.0
        stage.opacity = 0.0
        stage.show()

        Platform.runLater {
            ShortcutEditor().showDialog(stage, ShortcutSettings.load(emptyMap()))
            Platform.exit()
        }
    }

}
