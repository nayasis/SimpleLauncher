package tfx;

import io.nayasis.common.basica.file.Files;
import javafx.application.Application;
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


@Slf4j
public class MarkdownHelpTest extends Application {

    @Override
    public void start( Stage stage ) throws Exception {

        String html = parseMarkdown( "/view/markdown/sample.md" );

        WebView browser = getBrowser( html );

        StackPane pane = new StackPane();
        pane.getChildren().add( browser );

        stage.setTitle( "Markdown Test" );
        stage.setWidth( 500 );
        stage.setHeight( 500 );

        Scene scene = new Scene( pane );
        stage.setScene( scene );

        stage.show();

    }

    @NotNull
    private WebView getBrowser( String html ) {
        WebView browser = new WebView();
        WebEngine webEngine = browser.getEngine();
        webEngine.setUserStyleSheetLocation( getClass().getResource( "/view/markdown/github-markdown.css" ).toString() );
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
