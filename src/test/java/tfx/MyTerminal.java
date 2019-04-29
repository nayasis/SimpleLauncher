package tfx;

import com.kodedu.terminalfx.TerminalView;
import com.kodedu.terminalfx.annotation.WebkitCall;
import com.kodedu.terminalfx.config.TerminalConfig;
import com.kodedu.terminalfx.helper.IOHelper;
import com.kodedu.terminalfx.helper.ThreadHelper;
import com.pty4j.PtyProcess;
import com.pty4j.WinSize;
import io.nayasis.common.basica.etc.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.paint.Color;
import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.LinkedBlockingQueue;

@Slf4j
public class MyTerminal extends MyTerminalView {

    private final ObjectProperty<Writer> outputWriterProperty = new SimpleObjectProperty<>();
    private final Path terminalPath;
    private String[] termCommand;
    private final LinkedBlockingQueue<String> commandQueue = new LinkedBlockingQueue<>();

    public MyTerminal() {
        this(null, null);
    }

    public MyTerminal( TerminalConfig terminalConfig, Path terminalPath ) {

        terminalConfig = new TerminalConfig();
        terminalConfig.setFontSize( 10 );
//        terminalConfig.setFontFamily( "NanumGothic" );
//        terminalConfig.setScrollbarVisible( false );

//        terminalConfig.setCursorColor( Color.AQUA );

        terminalConfig.setCopyOnSelect( true );

        terminalConfig.setEnableClipboardNotice( false );

        setTerminalConfig(terminalConfig);
        this.terminalPath = terminalPath;
    }

    public MyTerminal setCommand( String command ) {
        termCommand = command.split( " " );
        return this;
    }

    @WebkitCall
    public void sendCommand( String command ) {
        try {
            commandQueue.put(command);
        } catch (final InterruptedException e) {
            throw new RuntimeException(e);
        }
        ThreadHelper.start(() -> {
            try {
                final String commandToExecute = commandQueue.poll();
                getOutputWriter().write(commandToExecute);
                getOutputWriter().flush();
            } catch (final IOException e) {
                e.printStackTrace();
            }
        });
    }

    @Override
    public void onTerminalReady() {

        javafx.application.Platform.runLater( () -> {
            try {
                initializeProcess();
            } catch (final Exception e) {}
        } );

//        ThreadHelper.start(() -> {
//            try {
//                initializeProcess();
//            } catch (final Exception e) {
//            }
//        });
    }

    private void initializeProcess() throws Exception {

        ProcessBuilder builder = new ProcessBuilder( termCommand );

        Process process = builder.start();

        setInputReader(new BufferedReader(new InputStreamReader( process.getInputStream(), Platform.osCharset )));
        setErrorReader(new BufferedReader(new InputStreamReader( process.getErrorStream(), Platform.osCharset)));
//        setOutputWriter(new BufferedWriter(new OutputStreamWriter( process.getOutputStream(), Platform.osCharset)));

//        focusCursor();

        countDownLatch.countDown();

        process.waitFor();

        System.out.println( ">> process end !!" );

    }

    private Path getDataDir() {
        final String userHome = System.getProperty("user.home");
        final Path dataDir = Paths.get(userHome).resolve(".terminalfx");
        return dataDir;
    }

    public Path getTerminalPath() {
        return terminalPath;
    }

    public ObjectProperty<Writer> outputWriterProperty() {
        return outputWriterProperty;
    }

    public Writer getOutputWriter() {
        return outputWriterProperty.get();
    }

    public void setOutputWriter(Writer writer) {
        outputWriterProperty.set(writer);
    }

}
