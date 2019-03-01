package com.nayasis.simplelauncher.controller;

import javafx.fxml.FXML;
import javafx.scene.control.ProgressBar;
import javafx.scene.effect.Glow;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class SplashController {

    @FXML
    protected ProgressBar progressBar;

    @FXML protected Text textStatus;

    @FXML
    private void initialize() {

        log.debug( "is it initialized ??" );

        textStatus.setFill( Color.WHITE );
        textStatus.setFont( Font.font("System", 14) );

        textStatus.setEffect( new Glow(1.0) );

    }

}
