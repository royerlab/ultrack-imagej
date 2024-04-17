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
import ij.WindowManager;
import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.layout.Border;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.stage.Stage;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;

public class ImageSelector extends JDialog {
    private final String[] requestedImages;
    private final JComboBox<String>[] imageComboBoxesSwing;

    public ImageSelector(String [] requestedImages) {
        super();
        this.requestedImages = requestedImages;
        this.imageComboBoxesSwing = new JComboBox[this.requestedImages.length];

        this.setTitle("Image Selector");
        this.setModalityType(java.awt.Dialog.ModalityType.APPLICATION_MODAL);

        this.setLayout(new BorderLayout(10, 10));
        // add border to the dialog (insets are the spaces between the border and the content)
        JPanel contentPane = (JPanel) this.getContentPane();
        contentPane.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JLabel lbTitle = new JLabel("Select an image for each selector");
        // set font with size 20
        lbTitle.setFont(lbTitle.getFont().deriveFont(18.0f));
        this.add(lbTitle, BorderLayout.NORTH);

        JPanel pnSelectors = new JPanel();
        pnSelectors.setLayout(new GridLayout(this.requestedImages.length, 2, 5, 5));

        String[] availableImages = WindowManager.getImageTitles();

        for (int i = 0; i < this.requestedImages.length; i++) {
            imageComboBoxesSwing[i] = new JComboBox<>(availableImages);
            JLabel label = new JLabel(this.requestedImages[i]);

            pnSelectors.add(label);
            pnSelectors.add(imageComboBoxesSwing[i]);
        }

        this.add(pnSelectors, BorderLayout.CENTER);

        JPanel buttonsPanel = new JPanel();
        buttonsPanel.setLayout(new FlowLayout(FlowLayout.RIGHT));
        JButton okButton = new JButton("OK");
        JButton cancelButton = new JButton("Cancel");
        buttonsPanel.add(okButton);
        buttonsPanel.add(cancelButton);
        this.add(buttonsPanel, BorderLayout.SOUTH);

        okButton.addActionListener(e -> {
            try {
                getSelectedImages();
                this.dispose();
            } catch (IllegalArgumentException ex) {
                JOptionPane.showMessageDialog(this, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                throw ex;
            }
        });

        cancelButton.addActionListener(e -> this.dispose());

        this.pack();
        this.setVisible(true);
    }

    public ArrayList<String[]> getSelectedImages() {
        ArrayList<String[]> pairs = new ArrayList<>();
        for (int i = 0; i < imageComboBoxesSwing.length; i++) {
            if (imageComboBoxesSwing[i].getSelectedItem() == null) {
                throw new IllegalArgumentException("Please select an image for each selector.\nYou missed " + this.requestedImages[i] + ".");
            }
            String image = (String) imageComboBoxesSwing[i].getSelectedItem();
            String path = WindowManager.getImage(image).getOriginalFileInfo().getFilePath();
            pairs.add(new String[]{this.requestedImages[i], path});
        }
        return pairs;
    }
}
