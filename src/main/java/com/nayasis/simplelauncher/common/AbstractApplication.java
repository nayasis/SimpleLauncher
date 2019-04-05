package com.nayasis.simplelauncher.common;

import com.nayasis.simplelauncher.Main;
import io.nayasis.common.ui.javafx.application.NApplication;
import io.nayasis.common.ui.javafx.dialog.Dialog;
import javafx.application.Platform;
import javafx.stage.Stage;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;

public abstract class AbstractApplication extends NApplication {

    protected ConfigurableApplicationContext context;

    private Exception initError = null;

    @Override
    public void init() {
        try {
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
            start();
        } catch ( Exception e ) {
            showError( e );
        }
    }

    protected void showError( Throwable e ) {
        Platform.runLater( () -> {
            e.printStackTrace( System.err );
            Dialog.error( e );
            closePreloader();
        });
    }

    protected abstract void start() throws Exception;

}
