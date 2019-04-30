package com.nayasis.simplelauncher.service.terminal.helper;

import io.nayasis.common.basicafx.javafx.etc.FxThread;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.TextInputDialog;
import javafx.scene.paint.Color;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * @see <a>https://github.com/javaterminal/TerminalFX</a>
 * Created by usta on 26.09.2016.
 */
public class FxHelper {

    public static String colorToHex(Color color) {
        return String.format("#%02X%02X%02X",
                (int) (color.getRed() * 255),
                (int) (color.getGreen() * 255),
                (int) (color.getBlue() * 255));
    }

    public static boolean askQuestion(String message) {
        CompletableFuture<Boolean> completableFuture = new CompletableFuture<Boolean>();
        completableFuture.runAsync(() -> FxThread.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION, message, ButtonType.YES, ButtonType.NO);
            ButtonType buttonType = alert.showAndWait().orElse(ButtonType.NO);
            completableFuture.complete(buttonType == ButtonType.YES);
        }) );

        return completableFuture.join();
    }

    public static String askInput(String message) {
        CompletableFuture<String> completableFuture = new CompletableFuture<>();
        completableFuture.runAsync(() -> {
            FxThread.runLater(() -> {
                TextInputDialog inputDialog = new TextInputDialog();
                inputDialog.setContentText(message);
                Optional<String> optional = inputDialog.showAndWait();
                completableFuture.complete(optional.orElse( null ));
            });
        });

        return completableFuture.join();
    }
}
