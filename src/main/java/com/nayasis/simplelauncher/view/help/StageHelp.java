package com.nayasis.simplelauncher.view.help;

import io.nayasis.common.basica.file.Files;
import io.nayasis.common.basica.model.Messages;
import io.nayasis.common.basicafx.javafx.stage.ConfigurableStage;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Stage;
import lombok.extern.slf4j.Slf4j;
import org.commonmark.node.Node;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

@Slf4j
public class StageHelp {

    private static final String CSS_MARKDOWN = "/view/markdown/github-markdown.css";

    private ConfigurableStage stage = new ConfigurableStage();

    public StageHelp() {
        init();
    }

    private void init() {

        String html = parseMarkdown( "/view/help.md" );

        WebView browser = getBrowser( html );

        StackPane pane = new StackPane();
        pane.getChildren().add( browser );

        stage.setTitle( Messages.get("menu.help") );
        stage.setWidth( 500 );
        stage.setHeight( 500 );

        Scene scene = new Scene( pane );
        stage.setScene( scene );

    }

    public ConfigurableStage getStage() {
        return stage;
    }

    @NotNull
    private WebView getBrowser( String html ) {
        WebView browser = new WebView();
        WebEngine webEngine = browser.getEngine();
        webEngine.setUserStyleSheetLocation( getClass().getResource( CSS_MARKDOWN ).toString() );
        webEngine.loadContent( html );
        return browser;
    }

    private String parseMarkdown( String resourcePath ) {

        String filePath = getClass().getResource( resourcePath ).getPath();

        String markdownContent = Files.readFrom( filePath );

        Parser parser = Parser.builder().build();
        Node document = parser.parse( markdownContent );
        HtmlRenderer renderer = HtmlRenderer.builder().build();

        String html = renderer.render( document );

        log.debug( html );

        return html;

    }






}
