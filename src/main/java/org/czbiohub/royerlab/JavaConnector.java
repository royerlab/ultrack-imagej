package org.czbiohub.royerlab;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import fiji.plugin.trackmate.LoadTrackMatePlugIn;
import javafx.application.Platform;
import javafx.scene.Cursor;
import javafx.scene.control.Alert;
import javafx.stage.Modality;
import javafx.stage.Stage;
import netscape.javascript.JSObject;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.input.SAXBuilder;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.UUID;
import java.util.function.Consumer;

public class JavaConnector {
    private final JSObject javascriptConnector;
    private final String ultrackPath;
    private final Consumer<String> onLog;
    private final Consumer<String> onError;
    private final Stage owner;
    private UltrackConnector ultrackConnector;

    public JavaConnector(JSObject javascriptConnector, String ultrackPath, Consumer<String> onLog, Consumer<String> onError, Stage owner) {
        this.javascriptConnector = javascriptConnector;
        this.ultrackPath = ultrackPath;
        this.onLog = onLog;
        this.onError = onError;
        this.owner = owner;
    }

    @SuppressWarnings("unused")
    public void startUltrackServer() {
        System.out.println("Starting Ultrack Server");
        owner.getScene().setCursor(Cursor.WAIT);

        ultrackConnector = new UltrackConnector(ultrackPath);
        ultrackConnector.startServer();

        owner.getScene().setCursor(Cursor.DEFAULT);

        javascriptConnector.call("setPort", ultrackConnector.getPort());
        javascriptConnector.call("startServer", "Ultrack Server Started");
    }

    @SuppressWarnings("unused")
    public void stopUltrackServer() {
        System.out.println("Stopping Ultrack Server");
        owner.getScene().setCursor(Cursor.WAIT);
        ultrackConnector.stopServer();
        ultrackConnector = null;
        owner.getScene().setCursor(Cursor.DEFAULT);
        javascriptConnector.call("successfullyStopped");
    }

    @SuppressWarnings("unused")
    public void requestImages(String listAsJson) {
        Gson gson = new Gson();
        String[] imageList = gson.fromJson(listAsJson, String[].class);

        Stage newStage = new Stage();
        newStage.setTitle("Image Selector");

        newStage.initModality(Modality.APPLICATION_MODAL);
        newStage.initOwner(owner);
        System.out.println("Requesting images: ");
        for (String image : imageList) {
            System.out.println(image);
        }

        ImageSelector imageSelector = new ImageSelector(imageList);
        imageSelector.start(newStage);
        ArrayList<String[]> selectedImages = imageSelector.getSelectedImages();
        javascriptConnector.call("updateSelectedImages", gson.toJson(selectedImages));
    }

    @SuppressWarnings("unused")
    public void viewTracks(String xml) {
        // temporary save the xml file in the temp directory with a uuid name
        String tempDir = System.getProperty("java.io.tmpdir");
        UUID uuid = UUID.randomUUID();
        String tempFile = tempDir + "/" + uuid + ".xml";

        // modify the xml Trackmate.Settings.ImageData.filename to the correct path
        SAXBuilder saxBuilder = new SAXBuilder();
        try {
            InputStream stream = new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8));
            Document document = saxBuilder.build(stream);

            ImageSelector imageSelector = new ImageSelector(new String[]{"Image to visualize the tracks overlay"});
            imageSelector.start(new Stage());
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
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setHeaderText("Error loading the tracks");
            alert.setContentText(e.getMessage());
            alert.showAndWait();
        }
    }

    @SuppressWarnings("unused")
    public void connectToUltrackWebsocket(String url, String message) {
        ultrackConnector.connectToWebsocket(url, message, (String response) -> {
            // parse json response
            Gson gson = new Gson();
            JsonObject jsonObject = gson.fromJson(response, JsonObject.class);
            String err = jsonObject.get("err_log").getAsString();
            String out = jsonObject.get("std_log").getAsString();
            onLog.accept(out);
            onError.accept(err);
            Platform.runLater(() -> javascriptConnector.call("updateJson", response));

            if (jsonObject.get("status").getAsString().equals("success")) {
                Platform.runLater( () -> javascriptConnector.call("finishTracking", ""));

            }
        });
    }
}