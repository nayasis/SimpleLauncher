package com.nayasis.simplelauncher.service.terminal;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nayasis.simplelauncher.service.terminal.helper.WebkitCall;
import io.nayasis.common.basica.file.Files;
import io.nayasis.common.basicafx.javafx.etc.FxThread;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyIntegerProperty;
import javafx.beans.property.ReadOnlyIntegerWrapper;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.Pane;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import lombok.extern.slf4j.Slf4j;
import netscape.javascript.JSObject;

import java.io.Reader;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;

/**
 * @see <a>https://github.com/javaterminal/TerminalFX</a>
 */
@Slf4j
public class TerminalView extends Pane {

    protected final WebView                webView             = new WebView();
    private   final ReadOnlyIntegerWrapper columnsProperty     = new ReadOnlyIntegerWrapper(2000 );
    private   final ReadOnlyIntegerWrapper rowsProperty        = new ReadOnlyIntegerWrapper(1000 );
    private   final ObjectProperty<Reader> inputReaderProperty = new SimpleObjectProperty<>();
    private   final ObjectProperty<Reader> errorReaderProperty = new SimpleObjectProperty<>();
    protected final CountDownLatch         countDownLatch      = new CountDownLatch(1);
    private         TerminalConfig         terminalConfig      = new TerminalConfig();

    public TerminalView() {

        inputReaderProperty.addListener((observable, oldValue, newValue) -> FxThread.start(() -> printReader(newValue) ) );
        errorReaderProperty.addListener((observable, oldValue, newValue) -> FxThread.start(() -> printReader(newValue) ) );

        webView.getEngine().getLoadWorker().stateProperty().addListener((observable, oldValue, newValue) -> {
            getWindow().setMember("app", this );
        });
        webView.prefHeightProperty().bind(heightProperty());
        webView.prefWidthProperty().bind(widthProperty());

        WebEngine webEngine = webEngine();
        webEngine.loadContent( getContents() );

    }

    private String getContents() {

        String script = Files.readResourceFrom("/view/hterm/hterm_all.js");

        StringBuilder sb = new StringBuilder();

        Files.readResourceFrom("/view/hterm/hterm.html", readline -> {
            if( "<script src=\"hterm_all.js\"></script>".equals(readline) ) {
                sb.append( "<script>" );
                sb.append( script );
                sb.append( "</script>" );
            } else {
                sb.append( readline );
            }
        });

        return sb.toString();

    }

    @WebkitCall
    public String getPrefs() {
        try {
            return new ObjectMapper().writeValueAsString(getTerminalConfig());
        } catch(final Exception e) {
            throw new RuntimeException(e);
        }
    }

    @WebkitCall
    public void updatePrefs(TerminalConfig terminalConfig) {
        if(getTerminalConfig().equals(terminalConfig)) {
            return;
        }

        setTerminalConfig(terminalConfig);
        final String prefs = getPrefs();

        FxThread.runLater(() -> {
            try {
                getWindow().call("updatePrefs", prefs);
            } catch(final Exception e) {
                e.printStackTrace();
            }
        }, true);
    }

    @WebkitCall
    public void resizeTerminal(int columns, int rows) {
        columnsProperty.set(columns);
        rowsProperty.set(rows);
    }

    @WebkitCall
    public void onTerminalInit() {
        FxThread.runLater(() -> {
            getChildren().add(webView);
        }, true);
    }

    @WebkitCall
    public void onTerminalReady() {
        FxThread.start(() -> {
            try {
                focusCursor();
                countDownLatch.countDown();
            } catch(final Exception e) {
                log.error( e.getMessage(), e );
            }
        });
    }

    private void printReader(Reader bufferedReader) {
        try {
            int nRead;
            final char[] data = new char[1 * 1024];

            while((nRead = bufferedReader.read(data, 0, data.length)) != -1) {
                final StringBuilder builder = new StringBuilder(nRead);
                builder.append(data, 0, nRead);
                print(builder.toString());
            }

        } catch(final Exception e) {
            e.printStackTrace();
        }
    }

    @WebkitCall
    public void copy(String text) {
        final Clipboard clipboard = Clipboard.getSystemClipboard();
        final ClipboardContent clipboardContent = new ClipboardContent();
        clipboardContent.putString(text);
        clipboard.setContent(clipboardContent);
    }

    public void onTerminalFxReady(Runnable onReadyAction) {
        FxThread.start(() -> {
            FxThread.await( countDownLatch );
            if( Objects.nonNull(onReadyAction)) {
                FxThread.start(onReadyAction);
            }
        });
    }

    protected void print(String text) {
        FxThread.runLater(() -> {
            getTerminalIO().call("print", text);
        });
    }

    public void focusCursor() {
        FxThread.runLater(() -> {
            webView.requestFocus();
            getTerminal().call("focus");
        }, true);
    }

    private JSObject getTerminal() {
        return (JSObject) webEngine().executeScript("t");
    }

    private JSObject getTerminalIO() {
        return (JSObject) webEngine().executeScript("t.io");
    }

    public JSObject getWindow() {
        return (JSObject) webEngine().executeScript("window");
    }

    private WebEngine webEngine() {
        return webView.getEngine();
    }

    public TerminalConfig getTerminalConfig() {
        if(Objects.isNull(terminalConfig)) {
            terminalConfig = new TerminalConfig();
        }
        return terminalConfig;
    }

    public void setTerminalConfig( TerminalConfig terminalConfig ) {
        this.terminalConfig = terminalConfig;
    }

    public ReadOnlyIntegerProperty columnsProperty() {
        return columnsProperty.getReadOnlyProperty();
    }

    public int getColumns() {
        return columnsProperty.get();
    }

    public ReadOnlyIntegerProperty rowsProperty() {
        return rowsProperty.getReadOnlyProperty();
    }

    public int getRows() {
        return rowsProperty.get();
    }

    public ObjectProperty<Reader> inputReaderProperty() {
        return inputReaderProperty;
    }

    public Reader getInputReader() {
        return inputReaderProperty.get();
    }

    public void setInputReader(Reader reader) {
        inputReaderProperty.set(reader);
    }

    public ObjectProperty<Reader> errorReaderProperty() {
        return errorReaderProperty;
    }

    public Reader getErrorReader() {
        return errorReaderProperty.get();
    }

    public void setErrorReader( Reader reader ) {
        errorReaderProperty.set(reader);
    }

}