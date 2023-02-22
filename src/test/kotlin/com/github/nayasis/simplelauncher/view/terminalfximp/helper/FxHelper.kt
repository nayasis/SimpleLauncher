package com.github.nayasis.simplelauncher.view.terminalfximp.helper

import javafx.scene.control.Alert
import javafx.scene.control.ButtonType
import javafx.scene.control.TextInputDialog
import javafx.scene.paint.Color
import java.util.concurrent.CompletableFuture

object FxHelper {
    fun colorToHex(color: Color): String {
        return String.format(
            "#%02X%02X%02X",
            (color.red * 255).toInt(),
            (color.green * 255).toInt(),
            (color.blue * 255).toInt()
        )
    }

    fun askQuestion(message: String?): Boolean {
        val completableFuture = CompletableFuture<Boolean>()
        CompletableFuture.runAsync {
            ThreadHelper.runActionLater {
                val alert = Alert(
                    Alert.AlertType.INFORMATION,
                    message,
                    ButtonType.YES,
                    ButtonType.NO
                )
                val buttonType =
                    alert.showAndWait().orElse(ButtonType.NO)
                completableFuture.complete(buttonType == ButtonType.YES)
            }
        }
        return completableFuture.join()
    }

    fun askInput(message: String?): String? {
        val completableFuture = CompletableFuture<String?>()
        CompletableFuture.runAsync {
            ThreadHelper.runActionLater {
                val inputDialog = TextInputDialog()
                inputDialog.contentText = message
                val optional = inputDialog.showAndWait()
                completableFuture.complete(optional.orElseGet { null })
            }
        }
        return completableFuture.join()
    }
}