package com.nayasis.simplelauncher.service.terminal;

import com.nayasis.simplelauncher.service.terminal.helper.WebkitCall;
import io.nayasis.common.basica.base.Strings;
import io.nayasis.common.basica.cli.Command;
import io.nayasis.common.basica.etc.Platforms;
import io.nayasis.common.basicafx.javafx.dialog.Dialog;
import io.nayasis.common.basicafx.javafx.etc.FxThread;
import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.stage.Stage;
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
    private       String[]                    command;
    private       String                      workingDirectory;
    private       Stage                       stage;
    private       Runnable                    postAction;

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
        this.command          = Strings.nvl(command).split( " " );
        this.workingDirectory = workingDirectory;
        return this;
    }

    public Terminal setCommand( Command command ) {
        this.command = command.get().toArray( new String[0] );
        File workingDirectory = command.getWorkingDirectory();
        if( workingDirectory != null ) {
            this.workingDirectory = workingDirectory.getAbsolutePath();
        }
        return this;
    }

    public String getWorkingDirectory() {
        return workingDirectory;
    }

    public Terminal setWorkingDirectory( String workingDirectory ) {
        this.workingDirectory = workingDirectory;
        return this;
    }

    public Runnable getPostAction() {
        return postAction;
    }

    public Terminal setPostAction( Runnable postAction ) {
        this.postAction = postAction;
        return this;
    }

    public Stage getStage() {
        return stage;
    }

    public Terminal setStage( Stage stage ) {
        this.stage = stage;
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

    @WebkitCall
    @Override
    public void onTerminalReady() {
        FxThread.start(() -> {
            try {

                runProcess();

                if( stage != null ) {
                    Platform.runLater( () -> {
                        stage.setTitle( Strings.format( "{} (done)", stage.getTitle()) );
                    });
                }

                if( postAction != null ) {
                    postAction.run();
                }

            } catch ( final Exception e ) {
                log.error( e.getMessage(), e );
                Platform.runLater( () -> {
                    Dialog.error( "msg.error.003", e );
                });
            }
        });
    }

    private void runProcess() throws Exception {

        ProcessBuilder builder = new ProcessBuilder( command );

        if( Strings.isNotEmpty(workingDirectory) ) {
            builder.directory( new File(workingDirectory) );
        }

        Process process = builder.start();

        setInputReader(  new BufferedReader(new InputStreamReader(  process.getInputStream(),  Platforms.osCharset)) );
        setErrorReader(  new BufferedReader(new InputStreamReader(  process.getErrorStream(),  Platforms.osCharset)) );
        setOutputWriter( new BufferedWriter(new OutputStreamWriter( process.getOutputStream(), Platforms.osCharset)) );

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
