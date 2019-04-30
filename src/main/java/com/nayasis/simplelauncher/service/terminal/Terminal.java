package com.nayasis.simplelauncher.service.terminal;

import com.nayasis.simplelauncher.service.terminal.helper.WebkitCall;
import io.nayasis.common.basica.base.Strings;
import io.nayasis.common.basica.etc.Platform;
import io.nayasis.common.basicafx.javafx.etc.FxThread;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * @see <a>https://github.com/javaterminal/TerminalFX</a>
 */
@Slf4j
public class Terminal extends TerminalView {

    private final ObjectProperty<Writer>      outputWriterProperty = new SimpleObjectProperty<>();
    private final LinkedBlockingQueue<String> commandQueue         = new LinkedBlockingQueue<>();
    private       String[] command;
    private       String   workingDirectory;

    public Terminal() {
        this(null);
    }

    public Terminal( TerminalConfig terminalConfig ) {

        if( terminalConfig == null ) {
            terminalConfig = new TerminalConfig();
            terminalConfig.setFontSize( 10 );
            terminalConfig.setCopyOnSelect( true );
            terminalConfig.setEnableClipboardNotice( false );
        }

        setTerminalConfig(terminalConfig);

    }

    public Terminal setCommand( String command ) {
        return setCommand( command, null );
    }

    public Terminal setCommand( String command, String workingDirectory ) {
        this.command          = Strings.nvl(command).split( " " );
        this.workingDirectory = workingDirectory;
        return this;
    }

    @WebkitCall
    public void sendCommand( String command ) {
        try {
            commandQueue.put( command );
        } catch (final InterruptedException e) {
            throw new RuntimeException(e);
        }
        FxThread.start(() -> {
            try {
                final String commandToExecute = commandQueue.poll();
                getOutputWriter().write(commandToExecute);
                getOutputWriter().flush();
            } catch (final IOException e) {
                log.error( e.getMessage(), e );
            }
        });
    }

    @Override
    public void onTerminalReady() {
        FxThread.start(() -> {
            try {
                runProcess();
            } catch ( final Exception e ) {}
        });
    }

    private void runProcess() throws Exception {

        ProcessBuilder builder = new ProcessBuilder( command );

        if( Strings.isNotEmpty(workingDirectory) ) {
            builder.directory( new File(workingDirectory) );
        }

        Process process = builder.start();

        setInputReader(  new BufferedReader(new InputStreamReader(  process.getInputStream(),  Platform.osCharset)) );
        setErrorReader(  new BufferedReader(new InputStreamReader(  process.getErrorStream(),  Platform.osCharset)) );
        setOutputWriter( new BufferedWriter(new OutputStreamWriter( process.getOutputStream(), Platform.osCharset)) );

        focusCursor();

        countDownLatch.countDown();

        process.waitFor();

        getInputReader().close();
        getErrorReader().close();
        getOutputWriter().close();

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
