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

import javax.swing.*;

/**
 * org.czbiohub.royerlab.AppMenu class is a JMenuBar that contains the menu items for the application.
 * <p>
 * The menu items are:
 * <ul>
 * <li><b>File</b>, which contains the exit menu item.</li>
 * <li><b>Environment</b>, which contains the information about the current conda environment to execute ultrack and an
 * option to select a different conda environment.</li>
 * <li><b>Help</b>, which contains the about menu item.</li>
 * </ul>
 * <p>
 * The menu items are added to the JMenuBar and the actions are defined for each menu item.
 */
public abstract class AppMenu extends JMenuBar {

    public JMenuItem exitMenu;

    private static String setCondaEnv() {
        String currentCondaEnv = CondaEnvironmentFinder.getCurrentCondaEnv();
        if (currentCondaEnv == null) {
            currentCondaEnv = "No conda env selected";
        }
        return currentCondaEnv;
    }

    /**
     * Constructor for the org.czbiohub.royerlab.AppMenu class.
     * <p>
     * The constructor initializes the menu items for the application and adds them to the JMenuBar.
     */
    public AppMenu() {
        JMenu fileMenu = new JMenu("File");
        JMenu envMenu = new JMenu("Environment");
        JMenu helpMenu = new JMenu("Help");

        // add file menu
        exitMenu = new JMenuItem("Exit");
        exitMenu = new JMenuItem("Exit");

        fileMenu.add(exitMenu);
        exitMenu.addActionListener(actionEvent -> {
            onExit();
        });

        // add environment menu
        JMenuItem currentConda = new JMenuItem();
        JMenuItem selectCondaPathMenu = new JMenuItem("Select Conda Path");

        String currentCondaEnv = setCondaEnv();
        currentConda.setText(currentCondaEnv);

        envMenu.add(currentConda);
        envMenu.add(selectCondaPathMenu);

        // Action
        selectCondaPathMenu.addActionListener(actionEvent -> {
            selectCondaPathMenu.setEnabled(false);
            new Thread(() -> {
                try {
                    String path = CondaEnvironmentFinder.openDialogToFindUltrack();
                    String updatedEnv = setCondaEnv();
                    currentConda.setText(updatedEnv);
                    System.out.println("Selected conda environment: " + path);
                    selectCondaPathMenu.setEnabled(true);
                    if (path != null)
                        onUpdateCondaEnv();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }).start();
        });

        currentConda.addActionListener(actionEvent -> {
            String title = "Current Conda Environment";
            String message = currentConda.getText() + " is selected as the current conda environment located at " + CondaEnvironmentFinder.getCurrentCondaPath();
            JOptionPane.showMessageDialog(null, message, title, JOptionPane.INFORMATION_MESSAGE);
        });

        {
            // Help menu
            helpMenu.add(new JMenuItem("About"));
        }

        this.add(fileMenu);
        this.add(envMenu);
        this.add(helpMenu);
    }

    public abstract void onExit();

    public abstract void onUpdateCondaEnv();
}
