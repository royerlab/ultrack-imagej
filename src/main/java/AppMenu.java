/*-
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
import javax.swing.*;

public class AppMenu extends JMenuBar {

    public JMenuItem exitMenu;

    private String setCondaEnv() {
        String currentCondaEnv = CondaEnvironmentFinder.getCurrentCondaEnv();
        if (currentCondaEnv == null) {
            currentCondaEnv = "No conda env selected";
        }
        return currentCondaEnv;
    }

    public AppMenu() {
        JMenu fileMenu = new JMenu("File");
        JMenu envMenu = new JMenu("Environment");
        JMenu helpMenu = new JMenu("Help");

        // File menu
        {
            // Initialization
            exitMenu = new JMenuItem("Exit");
            exitMenu = new JMenuItem("Exit");

            // Addition
            fileMenu.add(exitMenu);

            // Action

        }

        // Environment menu
        {
            // Initialization
            JMenuItem currentConda = new JMenuItem();
            JMenuItem selectCondaPathMenu = new JMenuItem("Select Conda Path");

            String currentCondaEnv = setCondaEnv();
            currentConda.setText(currentCondaEnv);
            //currentConda.setStyle("-fx-text-fill: gray;");

            // Addition
            envMenu.add(currentConda);
            envMenu.add(selectCondaPathMenu);

            // Action
            selectCondaPathMenu.addActionListener(actionEvent -> {
                try {
                    String path = CondaEnvironmentFinder.openDialogToFindUltrack();
                    String updatedEnv = setCondaEnv();
                    currentConda.setText(updatedEnv);
                    System.out.println("Selected conda environment: " + path);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            });

            currentConda.addActionListener(actionEvent -> {
                String title = "Current Conda Environment";
                String message = currentConda.getText() + " is selected as the current conda environment located at " + CondaEnvironmentFinder.getCurrentCondaPath();
                JOptionPane.showMessageDialog(null, message, title, JOptionPane.INFORMATION_MESSAGE);
            });
        }

        {
            // Help menu
            helpMenu.add(new JMenuItem("About"));
        }

        this.add(fileMenu);
        this.add(envMenu);
        this.add(helpMenu);
    }
}
