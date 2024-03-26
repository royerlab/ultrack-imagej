package org.czbiohub.royerlab;

import ij.WindowManager;
import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.stage.Stage;

import java.util.ArrayList;

public class ImageSelector extends Application {
    private final String[] requestedImages;
    private final ComboBox<String>[] imageComboBoxes;

    public ImageSelector(String [] requestedImages) {
        this.requestedImages = requestedImages;
        this.imageComboBoxes = new ComboBox[this.requestedImages.length];
    }

    @Override
    public void start(Stage primaryStage) {
        // Creating the dialog
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Image Selector");

        dialog.setHeaderText("Select an image for each selector");

        // Setting the button types.
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        String[] availableImages = WindowManager.getImageTitles();

        // Layout for the ComboBoxes
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 10, 10, 10));

        for (int i = 0; i < this.requestedImages.length; i++) {
            imageComboBoxes[i] = new ComboBox<>(FXCollections.observableArrayList(availableImages));
            Label label = new Label(this.requestedImages[i]);

            imageComboBoxes[i].setMaxWidth(Double.MAX_VALUE);
            GridPane.setHgrow(imageComboBoxes[i], javafx.scene.layout.Priority.ALWAYS);

            grid.add(label, 0, i * 2);
            grid.add(imageComboBoxes[i], 0, i * 2 + 1);
        }

        // Column constraints to make the column grow
        ColumnConstraints columnConstraints = new ColumnConstraints();
        columnConstraints.setFillWidth(true); // Allow the column to grow
        columnConstraints.setHgrow(Priority.ALWAYS); // Make the column grow horizontally
        grid.getColumnConstraints().add(columnConstraints); // Apply the column constraints to the grid

        dialog.getDialogPane().setContent(grid);

        // Handling the dialog result
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == ButtonType.OK) {
                try {
                    getSelectedImages();
                } catch (IllegalArgumentException ex) {
                    Alert alert = new Alert(Alert.AlertType.ERROR);
                    alert.setTitle("Error");
                    alert.setHeaderText("Image selection error");
                    alert.setContentText(ex.getMessage());
                    alert.showAndWait();
                    throw ex;
                }
            }
            return null;
        });

        // Show the dialog and wait for the user response
        dialog.showAndWait();
    }

    public ArrayList<String[]> getSelectedImages() {
        ArrayList<String[]> pairs = new ArrayList<>();
        for (int i = 0; i < imageComboBoxes.length; i++) {
            if (imageComboBoxes[i].getValue() == null) {
                throw new IllegalArgumentException("Please select an image for each selector.\nYou missed " + this.requestedImages[i] + ".");
            }
            String image = imageComboBoxes[i].getValue();
            String path = WindowManager.getImage(image).getOriginalFileInfo().getFilePath();
            pairs.add(new String[]{this.requestedImages[i], path});
        }
        return pairs;
    }
}