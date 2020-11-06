package com.nayasis.simplelauncher.view.preloader;

import com.github.nayasis.basicafx.javafx.preloader.NPreLoader;
import com.github.nayasis.basicafx.javafx.stage.ConfigurableStage;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

public class Splash extends NPreLoader {

    @Override
    public void start( Stage primaryStage ) throws Exception {

        Stage      stage = new ConfigurableStage( StageStyle.TRANSPARENT );
        AnchorPane root  = new AnchorPane();
        Scene      scene = new Scene( root, 527, 297 );

        root.setId( "splash" );
        scene.getStylesheets().add( "/view/splash.css" );
        stage.setScene( scene );
        stage.setAlwaysOnTop( true );

        root.getChildren().add( getStatusLayout() );

        stage.show();

        setStage( stage );

    }

    private VBox getStatusLayout() {

        Label       label       = new Label();
        ProgressBar progressBar = new ProgressBar();

        setHandler( ( message, percentage ) -> {
            if( message != null )
                label.setText( message );
            if( percentage != null )
                progressBar.progressProperty().set( percentage / 100. );
        });

        progressBar.setMaxWidth( Double.MAX_VALUE );

        VBox layout = new VBox();
        layout.setAlignment( Pos.CENTER_RIGHT );

        AnchorPane.setLeftAnchor(   layout, 0. );
        AnchorPane.setRightAnchor(  layout, 0. );
        AnchorPane.setBottomAnchor( layout, 0. );

        layout.getChildren().addAll( progressBar, label );

        return layout;

    }


}
