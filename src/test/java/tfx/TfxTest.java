package tfx;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class TfxTest extends Application {

    @Override
    public void start( Stage stage ) throws Exception {

        stage.setTitle( "TerminalFx test" );

        MyTerminal myTerminal = new MyTerminal();
        stage.setScene( new Scene( myTerminal, 300, 400 ) );

        stage.show();

        myTerminal.onTerminalFxReady( () -> {
            myTerminal.sendCommand( "dir\r" );
        });
    }

}
