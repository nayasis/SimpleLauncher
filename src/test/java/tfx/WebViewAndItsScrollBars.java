package tfx;

import javafx.application.Application;
import javafx.event.ActionEvent;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ScrollBar;
import javafx.scene.layout.VBox;
import javafx.scene.web.WebView;
import javafx.stage.Stage;

import java.util.Set;

import static javafx.application.Application.launch;

public class WebViewAndItsScrollBars extends Application {

    @Override
    public void start( Stage primaryStage) {

        // create webview and load content
        WebView view = new WebView();
        view.getEngine().load(
            "https://docs.oracle.com/javase/8/javafx/api/javafx/scene/control/ScrollBar.html");

        // vertical scrollbar of the webview
//        ScrollBar vScrollBar = getVScrollBar(view);

        // change scrollbar value, i.e., thumb position via button
        Button btn = new Button();
        btn.setText("Move ScrollBar");
        btn.setOnAction(( ActionEvent event) -> {
//            if (vScrollBar != null) {
//                double value = 2000;
//                System.out.println(">> current value: " + vScrollBar.getValue());
//                System.out.println(">> setting scrollbar value to " + value);
//                vScrollBar.setValue(value);
//            }
        });

        // create root layout
        VBox root = new VBox();
        root.setAlignment( Pos.CENTER);
        root.getChildren().add(view);
        root.getChildren().add(btn);

        // setup and show stage
        Scene scene = new Scene(root, 1024, 600);
        primaryStage.setTitle("Hello World!");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        launch(args);
    }

    /**
     * Returns the vertical scrollbar of the webview.
     *
     * @param webView webview
     * @return vertical scrollbar of the webview or {@code null} if no vertical
     * scrollbar exists
     */
    private ScrollBar getVScrollBar( WebView webView ) {

        Set<Node> scrolls = webView.lookupAll(".scroll-bar");
        for (Node scrollNode : scrolls) {

            if (ScrollBar.class.isInstance(scrollNode)) {
                ScrollBar scroll = (ScrollBar) scrollNode;
                if (scroll.getOrientation() == Orientation.VERTICAL) {
                    return scroll;
                }
            }
        }
        return null;
    }
}
