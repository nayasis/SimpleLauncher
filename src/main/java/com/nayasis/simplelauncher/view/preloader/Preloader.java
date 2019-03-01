package com.nayasis.simplelauncher.view.preloader;

import io.nayasis.common.file.Files;
import io.nayasis.common.ui.javafx.image.Images;
import io.nayasis.common.ui.javafx.preloader.NPreLoader;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.image.Image;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Background;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

public class Preloader extends NPreLoader {

    @Override
    public void start( Stage primaryStage ) throws Exception {

        Stage      stage = new Stage( StageStyle.UNDECORATED );
        AnchorPane root  = new AnchorPane();
        Scene      scene = new Scene( root, 600, 250 );

        root.setBackground( new Background( new Images().toBackgroundImage( "image/splash.jpg" ) ) );
        scene.getStylesheets().add( "/view/EmuPreLoader.css" );
        stage.setScene( scene );
        stage.getIcons().add( new Image( Files.getResource( "image/icon/favicon.png" ) ) );

        VBox statusLayout = getStatusLayout();

        root.getChildren().add( statusLayout );

        stage.show();

        setStage( stage );

    }

    private VBox getStatusLayout() {

        Label label = new Label();
        ProgressBar progressBar = new ProgressBar();

        setHandler( ( message, percentage ) -> {
            label.setText( message );
            progressBar.progressProperty().set( percentage );
        });

        label.getStyleClass().add( "label-status" );
        progressBar.setMaxWidth( Double.MAX_VALUE );

        VBox layout = new VBox();
        layout.setAlignment( Pos.CENTER_RIGHT );

        AnchorPane.setLeftAnchor( layout, 0. );
        AnchorPane.setRightAnchor( layout, 0. );
        AnchorPane.setBottomAnchor( layout, 0. );

        layout.getChildren().addAll( progressBar, label );

        return layout;

    }


}
