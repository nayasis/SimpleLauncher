package com.nayasis.simplelauncher;

import com.nayasis.simplelauncher.view.preloader.Splash;
import io.nayasis.common.ui.javafx.application.NApplication;
import io.nayasis.common.ui.javafx.dialog.Dialog;
import io.nayasis.common.ui.javafx.stage.ConfigurableStage;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Rectangle2D;
import javafx.stage.Screen;
import javafx.stage.Stage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

import static com.nayasis.simplelauncher.common.CONSTANT.STAGE.HELP;
import static com.nayasis.simplelauncher.common.CONSTANT.STAGE.MAIN;


@SpringBootApplication
@Slf4j
public class Main extends NApplication {

    private ConfigurableApplicationContext context;

    private Exception initError = null;

    public static void main( String... args ) {
        addDefaultIcon( "/image/icon/favicon.png" );
        setPreloader( Splash.class );
        Application.launch( Main.class, args );
    }

    @Override
    public void stop() {
        context.close();
    }

    @Override
    public void init() throws Exception {
        try {
            context = SpringApplication.run( Main.class, getRawParameters() );
            setDefaultControllerFactory( context::getBean );
        } catch ( Exception e ) {
            initError = e;
        }
    }

    @Override
    public void start( Stage primaryStage ) throws Exception {

        try {

            if( initError != null ) throw initError;

            HELP = new ConfigurableStage( "/view/Help.fxml" );
            MAIN = new ConfigurableStage( "/view/SimpleLauncher.fxml" );

            MAIN.setOnCloseRequest( event -> {
                HELP.close();
            });

            Rectangle2D screen = Screen.getPrimary().getVisualBounds();
            MAIN.setWidth( screen.getWidth() * 0.5 );
            MAIN.setHeight( screen.getHeight() * 0.5 );

            MAIN.centerOnScreen();

            new Thread( () -> {

                try {

                    dummyLogic();

                    throw new Exception( "MERONG ??" );

                } catch ( Exception e ) {
                    showError( e );
                }

            } ).start();

        } catch ( Exception e ) {
            showError( e );
        }

    }

    private void dummyLogic() throws InterruptedException {

        notifyPreloader( 10. );
        notifyPreloader( "10 percent" );

        Thread.sleep( 600 );

        notifyPreloader( 20. );
        notifyPreloader( "20 percent" );

        Thread.sleep( 600 );

        notifyPreloader( "Merong" );
        Thread.sleep( 600 );

    }

    private void showError( Throwable e ) {
        Platform.runLater( () -> {
            e.printStackTrace( System.err );
            Dialog.$.error( e );
            closePreloader();
        });
    }

}
