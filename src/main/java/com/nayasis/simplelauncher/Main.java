package com.nayasis.simplelauncher;

import com.nayasis.simplelauncher.view.preloader.Preloader;
import io.nayasis.common.model.Messages;
import io.nayasis.common.ui.javafx.dialog.Dialog;
import io.nayasis.common.ui.javafx.loader.FxmlLoader;
import io.nayasis.common.ui.javafx.preloader.PreloaderNotificator;
import io.nayasis.common.ui.javafx.stage.ConfigurableStage;
import javafx.application.Application;
import javafx.geometry.Rectangle2D;
import javafx.stage.Screen;
import javafx.stage.Stage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;


@SpringBootApplication
@Slf4j
public class Main extends Application {

    private ConfigurableApplicationContext context;

    private Exception initError = null;

    public static void main( String... args ) {
        setPreloader( Preloader.class );
        Application.launch( Main.class, args );
    }

    private static void setPreloader( Class<? extends Preloader> preloader ) {
        System.setProperty( "javafx.preloader", preloader.getName() );
    }

    @Override
    public void stop() {
        context.close();
    }

    @Override
    public void init() throws Exception {
        try {
            String[] arguments = getParameters().getRaw().toArray( new String[0] );
            context = SpringApplication.run( Main.class, arguments );
            FxmlLoader.setDefaultControllerFactory( context::getBean );
        } catch ( Exception e ) {
            initError = e;
        }
    }

    @Override
    public void start( Stage primaryStage ) throws Exception {

        try {

            if( initError != null ) throw initError;

            ConfigurableStage stage = new ConfigurableStage().loadFxml( "/view/main.fxml" );

            Rectangle2D screen = Screen.getPrimary().getVisualBounds();
            stage.setWidth( screen.getWidth() * 0.5 );
            stage.setHeight( screen.getHeight() * 0.5 );

            stage.centerOnScreen();

            closePreloader();

            stage.show();

        } catch ( Exception e ) {
            closePreloader();
            Dialog.$.error( e, Messages.get("Fail on loading") );
            throw e;
        }

    }

    private void closePreloader() {
        notifyPreloader( new PreloaderNotificator().setClose() );
    }

}
