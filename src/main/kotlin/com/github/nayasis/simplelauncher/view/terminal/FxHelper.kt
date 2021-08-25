package com.github.nayasis.simplelauncher.view.terminal

import javafx.application.Platform
import javafx.scene.control.Alert
import javafx.scene.control.ButtonType
import javafx.scene.control.TextInputDialog
import javafx.scene.paint.Color
import java.util.concurrent.CompletableFuture

object FxHelper {

    fun askQuestion(message: String?): Boolean {
        val completableFuture = CompletableFuture<Boolean>()
        CompletableFuture.runAsync {
            Platform.runLater {
                val alert = Alert(
                    Alert.AlertType.INFORMATION,
                    message,
                    ButtonType.YES,
                    ButtonType.NO
                )
                val buttonType = alert.showAndWait().orElse(ButtonType.NO)
                completableFuture.complete(buttonType == ButtonType.YES)
            }
        }
        return completableFuture.join()
    }

    fun askInput(message: String?): String? {
        val completableFuture = CompletableFuture<String?>()
        CompletableFuture.runAsync {
            Platform.runLater {
                val inputDialog = TextInputDialog()
                inputDialog.contentText = message
                val optional = inputDialog.showAndWait()
                completableFuture.complete(optional.orElse(null))
            }
        }
        return completableFuture.join()
    }
}


fun Color.toHex(): String {
    return "#%02X%02X%02X".format(
        (red * 255).toInt(),
        (green * 255).toInt(),
        (blue * 255).toInt()
    )
}