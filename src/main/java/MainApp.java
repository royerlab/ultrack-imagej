import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.application.Application;
import javafx.concurrent.Worker;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.BorderPane;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Stage;
import netscape.javascript.JSObject;
import java.net.URL;
import java.util.Objects;

public class MainApp extends Application {
    /** for communication to the Javascript engine. */
    private JSObject javascriptConnector;

    /** for communication from the Javascript engine. */
    private JavaConnector javaConnector;
    private TextArea logAreaErr;
    private TextArea logAreaOut;
    private WebEngine webEngine;


    public static void main(String[] args) {
        ImageJ.main(args);

        ImagePlus imp = IJ.openImage("/data_lids/home/ilansilva/Desktop/Fluo-N2DL-HeLa-image2.tiff");
        imp.show();
        ImagePlus imp2 = IJ.openImage("/data_lids/home/ilansilva/Desktop/Fluo-N2DL-HeLa-label.tiff");
        imp2.show();

        Application.launch(MainApp.class, args);
    }

    private void onLoadUltrackPath(String ultrackPath, Stage primaryStage) {
        // get resource from the resources folder
        URL url = getClass().getResource("/index.html");

        // set up the listener
        webEngine.getLoadWorker().stateProperty().addListener((observable, oldValue, newValue) -> {
            if (Worker.State.SUCCEEDED == newValue) {
                // get the Javascript connector object.
                javascriptConnector = (JSObject) webEngine.executeScript("getJsConnector()");
                javaConnector = new JavaConnector(javascriptConnector, ultrackPath, this::log, this::logError, primaryStage);

                // set an interface object named 'javaConnector' in the web engine's page
                JSObject window = (JSObject) webEngine.executeScript("window");
                window.setMember("javaConnector", javaConnector);
            }
        });

        // now load the page
        assert url != null;
        webEngine.load(url.toString());
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        Group group = new Group();
        group.getChildren().add(new AppMenu());

        WebView webView = new WebView();
        webEngine = webView.getEngine();
        webEngine.load(Objects.requireNonNull(getClass().getResource("/loading.html")).toString());

        // Split the window into two parts
        SplitPane root = new SplitPane();
        BorderPane borderPane = new BorderPane();
        borderPane.setTop(group);
        borderPane.setCenter(root);

        // add a logger to the window on the right
        SplitPane logger = new SplitPane();
        BorderPane loggerPane = new BorderPane();
        loggerPane.setTop(new Label("Ultrack Server Log"));
        logger.setOrientation(javafx.geometry.Orientation.VERTICAL);
        logAreaOut = new TextArea();
        logAreaOut.setEditable(false);
        logAreaOut.setWrapText(true);
        logAreaOut.setStyle("-fx-background-color:  #EEEEEE; -fx-padding: 10;");
        loggerPane.setCenter(logAreaOut);
        logAreaErr = new TextArea();
        logAreaErr.setEditable(false);
        logAreaErr.setWrapText(true);
        logAreaErr.setStyle("-fx-background-color:  #EEEEEE; -fx-padding: 10; -fx-text-fill: red;");
        BorderPane loggerPaneErr = new BorderPane();
        loggerPaneErr.setTop(new Label("Ultrack Server Error Log"));
        loggerPaneErr.setCenter(logAreaErr);
        logger.setStyle("-fx-background-color: #EEEEEE; -fx-padding: 10;");
        logger.getItems().addAll(loggerPane, loggerPaneErr);

        root.getItems().addAll(webView, logger);
        root.setDividerPositions(0.7f, 0.3f);

        primaryStage.setScene(new Scene(borderPane, 800, 700));
        primaryStage.show();

        Task<Void> task = new Task<Void>() {
            @Override
            protected Void call() {
                try {
                    String path = CondaEnvironmentFinder.getUltrackPath();
                    Platform.runLater(() -> onLoadUltrackPath(path, primaryStage));
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                return null;
            }
        };
        new Thread(task).start();
    }

    private void log(String message) {
        Platform.runLater(() -> {
            logAreaOut.setText(message);
            logAreaOut.setScrollTop(Double.MAX_VALUE);
        });
    }

    private void logError(String message) {
        Platform.runLater(() -> {
            logAreaErr.setText(message);
            logAreaErr.setScrollTop(Double.MAX_VALUE);
        });
    }
}