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
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import fiji.plugin.trackmate.LoadTrackMatePlugIn;
import javafx.application.Platform;
import javafx.stage.Modality;
import javafx.stage.Stage;
import netscape.javascript.JSObject;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.input.SAXBuilder;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;

import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.UUID;
import java.util.function.Consumer;

public class JavaConnector {
    private final JSObject javascriptConnector;
    private final String ultrackPath;
    private final Consumer<String> onLog;
    private final Consumer<String> onError;
    private final Consumer<String> onServerLog;
    private UltrackConnector ultrackConnector;

    public JavaConnector(JSObject javascriptConnector, String ultrackPath, Consumer<String> onLog, Consumer<String> onError, Consumer<String> onServerLog) {
        this.javascriptConnector = javascriptConnector;
        this.ultrackPath = ultrackPath;
        this.onLog = onLog;
        this.onError = onError;
        this.onServerLog = onServerLog;
    }

    @SuppressWarnings("unused")
    public void startUltrackServer() {
        System.out.println("Starting Ultrack Server");

        ultrackConnector = new UltrackConnector(ultrackPath, onServerLog) {
            @Override
            public void onExecutionErrorAction() {
                Platform.runLater(() -> {
                    javascriptConnector.call("resetPage");
                });
            }
        };
        ultrackConnector.startServer();

        javascriptConnector.call("setPort", ultrackConnector.getPort());
        javascriptConnector.call("startServer", "Ultrack Server Started");
    }

    @SuppressWarnings("unused")
    public void stopUltrackServer() {
        System.out.println("Stopping Ultrack Server");
        ultrackConnector.stopServer();
        ultrackConnector = null;
        javascriptConnector.call("successfullyStopped");
    }

    @SuppressWarnings("unused")
    public void requestImages(String listAsJson) {
        Gson gson = new Gson();
        String[] imageList = gson.fromJson(listAsJson, String[].class);

        Stage newStage = new Stage();
        newStage.setTitle("Image Selector");

        newStage.initModality(Modality.APPLICATION_MODAL);
        System.out.println("Requesting images: ");
        for (String image : imageList) {
            System.out.println(image);
        }

        ImageSelector imageSelector = new ImageSelector(imageList);
        ArrayList<String[]> selectedImages = imageSelector.getSelectedImages();
        javascriptConnector.call("updateSelectedImages", gson.toJson(selectedImages));
    }

    @SuppressWarnings("unused")
    public void viewTracks(String xml) {
        // temporary save the xml file in the temp directory with a uuid name
        String tempDir = System.getProperty("java.io.tmpdir");
        UUID uuid = UUID.randomUUID();
        String tempFile = tempDir + "/" + uuid + ".xml";
        File file = new File(tempFile);

        // modify the xml Trackmate.Settings.ImageData.filename to the correct path
        SAXBuilder saxBuilder = new SAXBuilder();
        try {
            InputStream stream = new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8));
            Document document = saxBuilder.build(stream);

            ImageSelector imageSelector = new ImageSelector(new String[]{"Image to visualize the tracks overlay"});
            ArrayList<String[]> selectedImages = imageSelector.getSelectedImages();
            File path = new File(selectedImages.get(0)[1]);

            // Find the 'filename' element (assuming a certain structure)
            Element rootElement = document.getRootElement();
            Element settings = rootElement.getChild("Settings");
            Element imageData = settings.getChild("ImageData");

            // Modify the filename and folder
            imageData.setAttribute("folder", path.getParent());
            imageData.setAttribute("filename", path.getName());

            // Save the document back
            XMLOutputter xmlOutput = new XMLOutputter(Format.getPrettyFormat());
            xmlOutput.output(document, new FileWriter(tempFile));

            // open the trackmate plugin
            LoadTrackMatePlugIn loadTrackMatePlugIn = new LoadTrackMatePlugIn();
            loadTrackMatePlugIn.run(tempFile);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "Error loading the tracks: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    @SuppressWarnings("unused")
    public void connectToUltrackWebsocket(String url, String message) {
        ultrackConnector.connectToWebsocket(url, message, this::onMessageConsumer, this::onErrorConsumer, this::onCloseConsumer);
    }

    public void openBrowserWithUrl(String url) {
        System.out.println("Opening " + url);
        if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
            SwingUtilities.invokeLater(() -> {
                try {
                    Desktop.getDesktop().browse(new URI(url));
                } catch (IOException | URISyntaxException e) {
                    System.err.println(e.getMessage());
                }
            });
        }

    }

    private void onMessageConsumer(String response) {
        // parse json response
        Gson gson = new Gson();
        JsonObject jsonObject = gson.fromJson(response, JsonObject.class);
        String err = jsonObject.get("err_log").getAsString();
        String out = jsonObject.get("std_log").getAsString();
        onLog.accept(out);
        onError.accept(err);
        Platform.runLater(() -> javascriptConnector.call("updateJson", response));

        if (jsonObject.get("status").getAsString().equals("success")) {
            Platform.runLater(() -> javascriptConnector.call("finishTracking", ""));

        }
    }

    private void onErrorConsumer(String error) {
        onError.accept(error);
        JOptionPane.showMessageDialog(null, error, "Error", JOptionPane.ERROR_MESSAGE);
        Platform.runLater(() -> javascriptConnector.call("closeConnection"));
    }

    private void onCloseConsumer(String message) {
        onLog.accept(message);
        Platform.runLater(() -> javascriptConnector.call("closeConnection"));
    }
}
