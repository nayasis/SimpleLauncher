package simple;

import com.googlecode.lanterna.terminal.DefaultTerminalFactory;
import com.googlecode.lanterna.terminal.Terminal;
import com.googlecode.lanterna.terminal.swing.SwingTerminalFrame;
import com.jediterm.terminal.ui.JediTermWidget;
import com.jediterm.terminal.ui.settings.DefaultSettingsProvider;
import com.jediterm.terminal.ui.settings.SettingsProvider;
import com.pty4j.PtyProcess;
import io.nayasis.common.basica.cli.CommandExecutor;
import io.nayasis.common.basica.etc.Platform;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import pty.PtyMain.LoggingPtyProcessTtyConnector;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;

@Slf4j
public class ExecutorTest {

    public static void main( String... args ) throws IOException, InterruptedException {


        SettingsProvider settingsProvider=new DefaultSettingsProvider();
        JediTermWidget tw = new JediTermWidget(settingsProvider);
        JFrame jframe = new JFrame();
        jframe.setSize(1280,720);
        jframe.add(tw);
        jframe.setVisible(true);
        jframe.setTitle( "merong" );
        jframe.setFont( new Font( "MALGUN", 10, 10  ) );

        tw.setAutoscrolls( true );

//

        Map<String,String> envs = new HashMap<>( System.getenv() );
        String[] command = new String[] { "dir" };

        PtyProcess process = PtyProcess.exec( command, envs, null );

        LoggingPtyProcessTtyConnector ttyConnector = new LoggingPtyProcessTtyConnector( process, Charset.forName( Platform.osCharset ) );

        tw.setTtyConnector( ttyConnector );


//        TeeStream tee_stdout = new TeeStream(System.out, tw.getOutputStream());
//        TeeStream tee_stderr = new TeeStream(System.err, tw.getOutputErrorStream());
//
//        System.setOut(tee_stdout);
//        System.setErr(tee_stderr);

//        final JFrame frame = new JFrame("Test");
//        frame.setSize(300, 300);
//        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
//
//        final MultiColumnList list = new MultiColumnList("1", 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13);
//        list.setFixedColumnsMode(5);
//        frame.getContentPane().add(list);
//        frame.setVisible(true);
//
////        useLanterna();


    }

    private static void useLanterna() throws IOException {
        SwingTerminalFrame terminal = (SwingTerminalFrame) new DefaultTerminalFactory().createTerminal();

        terminal.setCursorVisible( true );

        CommandExecutor executor = new CommandExecutor();

        int[] row = new int[ 1 ];

        executor.run( "cmd /c c: && cd \"c:\\Windows\" && dir", line -> {
//        executor.run( "dir", line -> {
            for( char c : line.toCharArray() ) {
                terminal.putCharacter( c );
            }
            terminal.putCharacter( '\n' );
            terminal.flush();
            row[0] = row[0] + 1;
            terminal.setCursorPosition( 0, row[0] );
            log.debug( line );
        } );

        executor.waitFor();
    }

    private static void runTerminal() throws IOException {

        Terminal terminal = new DefaultTerminalFactory( System.out, System.in, Charset.forName( Platform.osCharset) )
//            .setForceTextTerminal( true )
            .createTerminal();

        terminal.flush();

    }

    @Test
    public void simple() throws IOException {

        Terminal terminal = new DefaultTerminalFactory().createTerminal();

        CommandExecutor executor = new CommandExecutor();

//        executor.run( "cmd /c c: && cd \"c:\\Windows\" && dir", line -> {
        executor.run( "dir", line -> {
            for( char c : line.toCharArray() ) {
                terminal.putCharacter( c );
            }
            terminal.flush();
            log.debug( line );
        } );

        executor.waitFor();

//        log.debug( "Done !!\n{}", output );

    }

}
