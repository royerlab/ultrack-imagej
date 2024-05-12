package org.czbiohub.royerlab;/*-
 * #%L
 * Ultrack: Large-Scale Multi-Hypotheses Cell Tracking Using Ultrametric Contours Maps.
 * %%
 * Copyright (C) 2010 - 2024 RoyerLab.
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
 * #L%
 */
import ij.ImageJ;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.concurrent.Worker;
import javafx.embed.swing.JFXPanel;
import javafx.scene.Scene;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import netscape.javascript.JSObject;

import javax.swing.*;
import java.awt.*;
import java.net.URL;

public class MainApp extends JFrame {
    /** for communication to the Javascript engine. */
    private JSObject javascriptConnector;

    /** for communication from the Javascript engine. */
    private JavaConnector javaConnector;
    private JTextArea logAreaErr;
    private JTextArea logAreaOut;
    private JTextArea internalLogArea;
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
        logAreaErr = new JTextArea();
        logAreaErr.setForeground(Color.RED);
        logAreaErr.setEditable(false);
        logAreaErr.setWrapStyleWord(true);
        logAreaErr.setLineWrap(true);
        internalLogArea = new JTextArea();
        internalLogArea.setEditable(false);
        internalLogArea.setWrapStyleWord(true);
        internalLogArea.setLineWrap(true);
        internalLogArea.setForeground(Color.GRAY);
        logger.add(new JLabel("Ultrack Log"));
        logger.add(new JScrollPane(logAreaOut));
        logger.add(new JLabel("Ultrack Error Log"));
        logger.add(new JScrollPane(logAreaErr));
        logger.add(new JLabel("Server API Log"));
        logger.add(new JScrollPane(internalLogArea));

        JFXPanel fxPanel = new JFXPanel();
        JSplitPane root = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, false, fxPanel, logger);
        root.setDividerLocation(0.7);
        root.setResizeWeight(1.0);


        AppMenu appMenu = new AppMenu() {
            @Override
            public void onExit() {
                dispose();
            }

            @Override
            public void onUpdateCondaEnv() {
                String path = null;
                try {
                    path = CondaEnvironmentFinder.getUltrackPath();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                String finalPath = path;
                Platform.runLater(() -> onLoadUltrackPath(finalPath));
            }
        };
        setJMenuBar(appMenu);

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
                            while (path == null) {
                                try {
                                    path = CondaEnvironmentFinder.getUltrackPath();
                                } catch (InterruptedException e) {
                                    throw new RuntimeException(e);
                                }
                                if (path == null) {
                                    JOptionPane.showMessageDialog(null, "You can't proceed without selecting the ultrack path", "Error", JOptionPane.ERROR_MESSAGE);
                                }
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
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
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
                javaConnector = new JavaConnector(javascriptConnector, ultrackPath, this::onLog, this::onLogError, this::onServerLog);

                // set an interface object named 'javaConnector' in the web engine's page
                JSObject window = (JSObject) webEngine.executeScript("window");
                window.setMember("javaConnector", javaConnector);
            }
        });

        // now load the page
        assert url != null;
        webEngine.load(url.toString());
    }

    private void onLog(String message) {
        SwingUtilities.invokeLater( () -> {
            logAreaOut.setText(message);
        });
    }

    private void onLogError(String message) {
        SwingUtilities.invokeLater( () -> {
            logAreaErr.setText(message);
        });
    }

    private void onServerLog(String message) {
        SwingUtilities.invokeLater( () -> {
            internalLogArea.append(message + "\n");
        });
    }

}
