package com.nayasis.simplelauncher;

import com.nayasis.simplelauncher.common.AbstractApplication;
import com.nayasis.simplelauncher.controller.ConfigController;
import io.nayasis.common.basica.model.Messages;
import io.nayasis.common.basicafx.javafx.stage.ConfigurableStage;
import javafx.application.Application;
import javafx.concurrent.Task;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.io.IOException;

import static com.nayasis.simplelauncher.common.CONSTANT.STAGE.HELP;
import static com.nayasis.simplelauncher.common.CONSTANT.STAGE.MAIN;

@SpringBootApplication
@Slf4j
public class Main extends AbstractApplication {

    public static Main $ = new Main();

    private final String APPLICATION_NAME = "Simple Launcher";

    @Autowired
    private ConfigController configController;

    public static void main( String... args ) {

        addDefaultIcon( "/image/icon/favicon.png" );
//        setPreloader( Splash.class );
        Application.launch( Main.class, args );

    }

    @Override
    protected void start( CommandLine commandLine ) {

        Messages.load( "message/**.prop" );

        if( commandLine.hasOption("help") || commandLine.hasOption("h") ) {
            printHelp();
            return;
        } else if( commandLine.hasOption( "clear" ) ) {
            configController.setRestoreMainStageProperties( false );
        }

        HELP = new ConfigurableStage( "/view/Help.fxml" );
        MAIN = new ConfigurableStage( "/view/SimpleLauncher.fxml" );

        MAIN.setTitle( APPLICATION_NAME );
        HELP.setTitle( APPLICATION_NAME );

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

    }

    private void dummyLogic() throws InterruptedException, IOException {

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

    @Override
    protected void setOptions( Options options ) {
        options.addOption( "h", false, "print Help." );
        options.addOption( "clear", false, "clear memorized UI setting." );
    }

    private void printHelp() {
        closePreloader();
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp( "Help...", options );
    }

}
