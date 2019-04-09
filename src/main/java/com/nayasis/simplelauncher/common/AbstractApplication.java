package com.nayasis.simplelauncher.common;

import com.nayasis.simplelauncher.Main;
import io.nayasis.common.ui.javafx.application.NApplication;
import io.nayasis.common.ui.javafx.dialog.Dialog;
import javafx.application.Platform;
import javafx.stage.Stage;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;

public abstract class AbstractApplication extends NApplication {

    protected ConfigurableApplicationContext context;

    protected static Options options = new Options();

    private Exception initError = null;

    @Override
    public void init() {
        try {
            setOptions( options );
            context = SpringApplication.run( Main.class, getRawParameters() );
            context.getAutowireCapableBeanFactory().autowireBean( this );
            setDefaultControllerFactory( context::getBean );
        } catch ( Exception e ) {
            initError = e;
        }
    }

    @Override
    public void stop() {
        context.close();
    }

    @Override
    public void start( Stage primaryStage ) throws Exception {
        try {
            if( initError != null ) throw initError;
            start( toCommandLine( options, getRawParameters() ) );
        } catch ( Exception e ) {
            showError( e );
        }
    }

    private CommandLine toCommandLine( Options options, String... arguments ) throws ParseException {
        CommandLineParser parser = new DefaultParser();
        return parser.parse( options, arguments );
    }

    protected void showError( Throwable e ) {
        Platform.runLater( () -> {
            e.printStackTrace( System.err );
            Dialog.error( e );
            closePreloader();
        });
    }

    protected abstract void start( CommandLine commandLine ) throws Exception;

    protected abstract void setOptions( Options options );

}
