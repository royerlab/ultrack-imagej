import com.sun.corba.se.spi.orbutil.threadpool.Work;
import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.concurrent.Task;
import javafx.application.Application;
import javafx.concurrent.Worker;
import javafx.embed.swing.JFXPanel;
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

import javax.swing.*;
import java.awt.*;
import java.net.URL;
import java.util.Objects;

public class MainApp extends JFrame {
    /** for communication to the Javascript engine. */
    private JSObject javascriptConnector;

    /** for communication from the Javascript engine. */
    private JavaConnector javaConnector;
    private JTextArea logAreaErr;
    private JTextArea logAreaOut;
    private WebEngine webEngine;

    public MainApp() {
        super("Ultrack");

        // Split the window into two parts
        JPanel logger = new JPanel();
        logger.setLayout(new BoxLayout(logger, BoxLayout.Y_AXIS));
        logAreaOut = new JTextArea();
        logAreaOut.setEditable(false);
        logAreaOut.setWrapStyleWord(true);
        logAreaOut.setLineWrap(true);
        JScrollPane scrollPaneOut = new JScrollPane(logAreaOut);
        logAreaErr = new JTextArea();
        logAreaErr.setForeground(Color.RED);
        logAreaErr.setEditable(false);
        logAreaErr.setWrapStyleWord(true);
        logAreaErr.setLineWrap(true);
        JScrollPane scrollPaneErr = new JScrollPane(logAreaErr);
        logger.add(new JLabel("Ultrack Server Log"));
        logger.add(scrollPaneOut);
        logger.add(new JLabel("Ultrack Server Error Log"));
        logger.add(scrollPaneErr);

        JFXPanel fxPanel = new JFXPanel();
        JSplitPane root = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, false, fxPanel, logger);
        root.setDividerLocation(0.7);
        root.setResizeWeight(1.0);
//        root.setOneTouchExpandable(true);
//        root.add(fxPanel);
//        root.add(logger);

        AppMenu appMenu = new AppMenu();
        setJMenuBar(appMenu);
        appMenu.exitMenu.addActionListener(actionEvent -> {
            this.dispose();
        });

        Platform.runLater(() -> {
            try {
                WebView webView = new WebView();
                fxPanel.setScene(new Scene(webView));
                webEngine = webView.getEngine();
                try {
                    Task<Void> task = new Task<Void>() {
                        @Override
                        protected Void call() {
                            String path = null;
                            try {
                                path = CondaEnvironmentFinder.getUltrackPath();
                            } catch (InterruptedException e) {
                                throw new RuntimeException(e);
                            }
                            String finalPath = path;
                            Platform.runLater(() -> onLoadUltrackPath(finalPath));
                            return null;
                        }
                    };
                    task.run();

                    webEngine.load(String.valueOf(getClass().getResource("/web/loading.html").toURI()));
                } catch (Exception e) {
                    e.printStackTrace();
                }



            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        add(root);
        setSize(800, 700);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);


    }


    public static void main(String[] args) {
        ImageJ.main(args);

        new MainApp().setVisible(true);
    }

    private void onLoadUltrackPath(String ultrackPath) {
        // get resource from the resources folder
        URL url = getClass().getResource("/web/index.html");

        // set up the listener
        webEngine.getLoadWorker().stateProperty().addListener((observable, oldValue, newValue) -> {
            if (Worker.State.SUCCEEDED == newValue) {
                // get the Javascript connector object.
                javascriptConnector = (JSObject) webEngine.executeScript("getJsConnector()");
                javaConnector = new JavaConnector(javascriptConnector, ultrackPath, this::log, this::logError);

                // set an interface object named 'javaConnector' in the web engine's page
                JSObject window = (JSObject) webEngine.executeScript("window");
                window.setMember("javaConnector", javaConnector);
            }
        });

        // now load the page
        assert url != null;
        webEngine.load(url.toString());
    }

    private void log(String message) {
        SwingUtilities.invokeLater( () -> {
            logAreaOut.setText(message);
//            logAreaOut.setScrollTop(Double.MAX_VALUE);
        });
    }

    private void logError(String message) {
        SwingUtilities.invokeLater( () -> {
            logAreaErr.setText(message);
//            logAreaErr.setScrollTop(Double.MAX_VALUE);
        });
    }
}