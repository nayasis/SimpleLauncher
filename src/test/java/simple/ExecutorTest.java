package simple;

import com.googlecode.lanterna.terminal.DefaultTerminalFactory;
import com.googlecode.lanterna.terminal.Terminal;
import com.googlecode.lanterna.terminal.swing.SwingTerminalFrame;
import com.jediterm.terminal.ui.JediTermWidget;
import com.jediterm.terminal.ui.settings.DefaultSettingsProvider;
import com.jediterm.terminal.ui.settings.SettingsProvider;
import io.nayasis.common.cli.CommandExecutor;
import io.nayasis.common.etc.Platform;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;

import javax.swing.*;
import java.io.IOException;
import java.nio.charset.Charset;

@Slf4j
public class ExecutorTest {

    public static void main( String... args ) throws IOException, InterruptedException {


        SettingsProvider settingsProvider=new DefaultSettingsProvider();
        JediTermWidget tw = new JediTermWidget(settingsProvider);
        JFrame jframe = new JFrame();
        jframe.setSize(1280,720);
        jframe.add(tw);
        jframe.setVisible(true);

        tw.setAutoscrolls( true );

//        TeeStream tee_stdout = new TeeStream(System.out, tw.getOutputStream());
//        TeeStream tee_stderr = new TeeStream(System.err, tw.getOutputErrorStream());
//
//        System.setOut(tee_stdout);
//        System.setErr(tee_stderr);

        System.out.println( "HELLO" );


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
