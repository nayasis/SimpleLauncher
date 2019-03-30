package com.nayasis.simplelauncher;

import com.nayasis.simplelauncher.controller.ConfigController;
import com.nayasis.simplelauncher.view.preloader.Splash;
import io.nayasis.common.model.Messages;
import io.nayasis.common.ui.javafx.application.NApplication;
import io.nayasis.common.ui.javafx.dialog.Dialog;
import io.nayasis.common.ui.javafx.stage.ConfigurableStage;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.stage.Stage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

import static com.nayasis.simplelauncher.common.CONSTANT.STAGE.HELP;
import static com.nayasis.simplelauncher.common.CONSTANT.STAGE.MAIN;

@SpringBootApplication
@Slf4j
public class Main extends NApplication {

    private final String APPLICATION_NAME = "Simple Launcher";

    @Autowired
    private ConfigController configController;

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
            context.getAutowireCapableBeanFactory().autowireBean( this );
            setDefaultControllerFactory( context::getBean );
        } catch ( Exception e ) {
            initError = e;
        }
    }

    @Override
    public void start( Stage primaryStage ) throws Exception {

        try {

            if( initError != null ) throw initError;

            Messages.load( "message/**.prop" );

            HELP = new ConfigurableStage( "/view/Help.fxml" );
            MAIN = new ConfigurableStage( "/view/SimpleLauncher.fxml" );

            HELP.setTitle( APPLICATION_NAME );
            MAIN.setTitle( APPLICATION_NAME );

            MAIN.setOnShowing( event -> {
                configController.restore();
            });

            MAIN.setOnCloseRequest( event -> {
                HELP.close();
                configController.save();
            });

            new Thread( new Task<Void>() {
                protected Void call() {
                    try {
                        dummyLogic();
                    } catch ( Exception e ) {
                        showError( e );
                    }
                    return null;
                }
            }).start();

        } catch ( Exception e ) {
            showError( e );
        }

    }

    private void dummyLogic() throws InterruptedException {

        notifyPreloader( 10. );
        notifyPreloader( "preloader.loadSpring" );

        notifyPreloader( 20. );
        notifyPreloader( "Load View" );

        notifyPreloader( 20. );
        notifyPreloader( "20 percent" );

        Thread.sleep( 600 );

        notifyPreloader( "Merong" );
        Thread.sleep( 600 );

        closePreloader();

        MAIN.showLater();

    }

    private void showError( Throwable e ) {
        Platform.runLater( () -> {
            e.printStackTrace( System.err );
            Dialog.error( e );
            closePreloader();
        });
    }

}
