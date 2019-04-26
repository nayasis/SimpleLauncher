package tfx;

import com.kodedu.terminalfx.TerminalView;
import com.kodedu.terminalfx.annotation.WebkitCall;
import com.kodedu.terminalfx.config.TerminalConfig;
import com.kodedu.terminalfx.helper.IOHelper;
import com.kodedu.terminalfx.helper.ThreadHelper;
import com.pty4j.PtyProcess;
import com.pty4j.WinSize;
import com.sun.jna.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
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
public class MyTerminal extends TerminalView {

    private PtyProcess process;
    private final ObjectProperty<Writer> outputWriterProperty = new SimpleObjectProperty<>();
    private final Path terminalPath;
    private String[] termCommand;
    private final LinkedBlockingQueue<String> commandQueue = new LinkedBlockingQueue<>();

    public MyTerminal() {
        this(null, null);
    }

    public MyTerminal( TerminalConfig terminalConfig, Path terminalPath ) {
        setTerminalConfig(terminalConfig);
        this.terminalPath = terminalPath;
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
        ThreadHelper.start(() -> {
            try {
                initializeProcess();
            } catch (final Exception e) {
            }
        });
    }

    private void initializeProcess() throws Exception {

        final Path dataDir = getDataDir();

        IOHelper.copyLibPty(dataDir);

        if ( Platform.isWindows()) {
            this.termCommand = getTerminalConfig().getWindowsTerminalStarter().split("\\s+");
        } else {
            this.termCommand = getTerminalConfig().getUnixTerminalStarter().split("\\s+");
        }

        final Map<String, String> envs = new HashMap<>( System.getenv() );
        System.setProperty("PTY_LIB_FOLDER", dataDir.resolve("libpty").toString());

        if ( Objects.nonNull(terminalPath) && Files.exists(terminalPath)) {
            this.process = PtyProcess.exec( termCommand, envs, terminalPath.toString());
        } else {
            this.process = PtyProcess.exec( termCommand, envs);
        }

        columnsProperty().addListener(evt -> updateWinSize());
        rowsProperty().addListener(evt -> updateWinSize());
        updateWinSize();
        setInputReader(new BufferedReader(new InputStreamReader(process.getInputStream())));
        setErrorReader(new BufferedReader(new InputStreamReader(process.getErrorStream())));
        setOutputWriter(new BufferedWriter(new OutputStreamWriter(process.getOutputStream())));
        focusCursor();

        countDownLatch.countDown();

        process.waitFor();

    }

    private Path getDataDir() {
        final String userHome = System.getProperty("user.home");
        final Path dataDir = Paths.get(userHome).resolve(".terminalfx");
        return dataDir;
    }

    public Path getTerminalPath() {
        return terminalPath;
    }

    private void updateWinSize() {
        try {
            process.setWinSize(new WinSize(getColumns(), getRows()));
        } catch (Exception e) {
            log.error( e.getMessage(), e );
        }
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

    public PtyProcess getProcess() {
        return process;
    }

}
