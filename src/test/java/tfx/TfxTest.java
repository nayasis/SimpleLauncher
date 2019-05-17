package tfx;

import com.nayasis.simplelauncher.service.terminal.Terminal;
import com.nayasis.simplelauncher.service.terminal.TerminalConfig;
import io.nayasis.common.basica.base.Strings;
import io.nayasis.common.basica.cli.Command;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class TfxTest extends Application {

    @Override
    public void start( Stage stage ) throws Exception {

        stage.setTitle( "TerminalFx test" );

        TerminalConfig config = new TerminalConfig();
        config.setBackgroundColor( Color.rgb(16, 16, 16));
        config.setForegroundColor(Color.rgb(240, 240, 240));
        config.setCursorColor(Color.rgb(255, 0, 0, 0.5));
        config.setScrollbarVisible( false );
        config.setFontSize( 12 );
        config.setScrollWhellMoveMultiplier( 3 );
        config.setEnableClipboardNotice( false );

//        String exec = "d:\\development\\ChdToPbp\\lib\\chdman.exe";
//        String chd  = "d:\\development\\ChdToPbp\\img\\102 Dalmatians - Puppies to the Rescue [NTSC-U] [SLUS-01152].chd";
//        String trg  = "d:\\development\\ChdToPbp\\_temp\\img.";
//
//        String command = Strings.format(
//            "\"{}\" extractcd -f -i \"{}\" -o \"{}\" -ob \"{}\" ",
//            exec, chd, trg + "cue", trg + "bin"
//        );

//        String command = "cmd /c c: && cd \"c:\\Windows\" && dir";

        Command command = new Command( "cmd /c c: && cd \"c:\\Windows\" && dir" );

        log.debug( command.toString() );

        Terminal myTerminal = new Terminal( config ).setCommand( command ).setStage( stage );
        stage.setScene( new Scene( myTerminal, 900, 600 ) );
        stage.show();

        stage.setOnCloseRequest( event -> {
            stage.close();
            System.exit( 0 );
        });
//        myTerminal.onTerminalFxReady( () -> {
////            myTerminal.sendCommand( "dir\r" );
//        });
    }

}
