package com.nayasis.simplelauncher.view.help;

import com.vladsch.flexmark.ext.abbreviation.AbbreviationExtension;
import com.vladsch.flexmark.ext.autolink.AutolinkExtension;
import com.vladsch.flexmark.ext.definition.DefinitionExtension;
import com.vladsch.flexmark.ext.gfm.strikethrough.StrikethroughExtension;
import com.vladsch.flexmark.ext.jekyll.tag.JekyllTagExtension;
import com.vladsch.flexmark.ext.tables.TablesExtension;
import com.vladsch.flexmark.ext.typographic.TypographicExtension;
import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.ast.Node;
import com.vladsch.flexmark.util.options.MutableDataSet;
import io.nayasis.basica.file.Files;
import io.nayasis.basica.model.Messages;
import io.nayasis.basicafx.javafx.stage.ConfigurableStage;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

import java.io.InputStream;
import java.util.Arrays;

@Slf4j
public class StageHelp extends ConfigurableStage {

    private final String CSS_MARKDOWN = "/view/markdown/github-markdown.css";

    private boolean initialized = false;

    private void init() {

        String html = parseMarkdown( "/view/help/help.md" );

        WebView browser = getBrowser( html );

        StackPane pane = new StackPane();
        pane.getChildren().add( browser );

        this.setTitle( Messages.get("menu.help") );
        this.setWidth( 700 );
        this.setHeight( 600 );

        Scene scene = new Scene( pane );
        this.setScene( scene );

        initialized = true;

    }

    @Override
    public ConfigurableStage showLater() {
        if( ! initialized ) init();
        return super.showLater();
    }

    @Override
    public void showAndWait() {
        if( ! initialized ) init();
        super.showAndWait();
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

        InputStream resource = Files.getResource( resourcePath );

        String markdownContent = Files.readFrom( resource );

        MutableDataSet option = getOption();

        Parser parser = Parser.builder( option ).build();
        Node document = parser.parse( markdownContent );
        HtmlRenderer renderer = HtmlRenderer.builder( option ).build();

        String html = "<html><body class='markdown-body'>" + renderer.render( document ) + "</body></html>";

        log.trace( html );

        return html;

    }

    private MutableDataSet getOption() {
        MutableDataSet options = new MutableDataSet();
        options.set( Parser.EXTENSIONS, Arrays.asList(
            TablesExtension.create(),
            StrikethroughExtension.create(),
            AbbreviationExtension.create(),
            DefinitionExtension.create(),
            TypographicExtension.create(),
            AutolinkExtension.create(),
            JekyllTagExtension.create()
        ));
        return options;
    }

}
