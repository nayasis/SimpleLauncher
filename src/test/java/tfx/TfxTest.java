package tfx;

import com.kodedu.terminalfx.TerminalBuilder;
import com.kodedu.terminalfx.TerminalTab;
import com.kodedu.terminalfx.config.TerminalConfig;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.TabPane;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

public class TfxTest extends Application {

    @Override
    public void start( Stage stage ) throws Exception {

        stage.setTitle( "TerminalFx test" );

        MyTerminal myTerminal = new MyTerminal().setCommand( "cmd /c c: && cd \"c:\\Windows\" && dir" );
//        MyTerminal myTerminal = new MyTerminal();
        stage.setScene( new Scene( myTerminal, 900, 800 ) );
        stage.show();

        myTerminal.onTerminalFxReady( () -> {
//            myTerminal.sendCommand( "dir\r" );
        });
    }

//    @Override
//    public void start( Stage stage ) throws Exception {
//
//        stage.setTitle( "TerminalFx test" );
//
//        TerminalConfig darkConfig = new TerminalConfig();
//        darkConfig.setBackgroundColor( Color.rgb(16, 16, 16));
//        darkConfig.setForegroundColor(Color.rgb(240, 240, 240));
//        darkConfig.setCursorColor(Color.rgb(255, 0, 0, 0.5));
//
//        TerminalBuilder terminalBuilder = new TerminalBuilder(darkConfig);
//        TerminalTab terminal = terminalBuilder.newTerminal();
//
//        TabPane tabPane = new TabPane();
//        tabPane.getTabs().add(terminal);
//
//        stage.setScene( new Scene( tabPane, 900, 800 ) );
//        stage.show();
//
//        terminal.onTerminalFxReady( () -> {
//            terminal.getTerminal().command( "cmd /c c: && cd \"c:\\Windows\" && dir\r" );
//        });
//    }

//    @Override
//    public void start( Stage stage ) throws Exception {
//
//        stage.setTitle( "TerminalFx test" );
//
//        TerminalBuilder builder = new TerminalBuilder();
//        TerminalTab terminalTab = builder.newTerminal();
//
//        TabPane tabPane = new TabPane();
//        tabPane.getTabs().add(terminalTab);
//
//        stage.setScene( new Scene( tabPane, 900, 800 ) );
//        stage.show();
//
//        terminalTab.onTerminalFxReady( () -> {
//            terminalTab.getTerminal().command( "cmd /c c: && cd \"c:\\Windows\" && dir\r" );
//        });
//    }

}
