package com.nayasis.simplelauncher;

import com.nayasis.simplelauncher.common.AbstractApplication;
import com.nayasis.simplelauncher.controller.ConfigController;
import com.nayasis.simplelauncher.view.help.StageHelp;
import com.nayasis.simplelauncher.view.preloader.Splash;
import io.nayasis.common.basica.model.Messages;
import io.nayasis.common.basicafx.javafx.stage.ConfigurableStage;
import javafx.application.Application;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import static com.nayasis.simplelauncher.common.CONSTANT.STAGE.HELP;
import static com.nayasis.simplelauncher.common.CONSTANT.STAGE.MAIN;

@SpringBootApplication
@Slf4j
public class Main extends AbstractApplication {

    public static Main $ = new Main();

    public static final String APPLICATION_NAME = "Simple Launcher";

    @Autowired
    private ConfigController configController;

    public static void main( String... args ) {

        addDefaultIcon( "/image/icon/favicon.ico" );
        setPreloader( Splash.class );
        Application.launch( Main.class, args );

    }

    @Override
    protected void start( CommandLine commandLine ) {

        notifyPreloader( 10. );
        notifyPreloader( "preloader.spring-core.loaded" );

        Messages.load( "message/**.prop" );

        if( commandLine.hasOption("help") || commandLine.hasOption("h") ) {
            printHelp();
            return;
        } else if( commandLine.hasOption( "clear" ) ) {
            configController.setRestoreMainStageProperties( false );
        }

        HELP = new StageHelp();
        MAIN = new ConfigurableStage( "/view/SimpleLauncher.fxml" );

        MAIN.setTitle( APPLICATION_NAME );
        MAIN.setOnShowing( event -> {
            configController.restore();
        });

        MAIN.setOnCloseRequest( event -> {
            HELP.close();
            configController.save();
        });

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
