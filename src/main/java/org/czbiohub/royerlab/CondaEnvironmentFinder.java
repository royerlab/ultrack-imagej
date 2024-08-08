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
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;
import java.util.prefs.Preferences;

public class CondaEnvironmentFinder extends JDialog {

    private final Object lock = new Object();
    private final JTextField condaPathField;
    private final JComboBox<CondaEnvironment> condaEnvComboBox;
    private boolean cancelled = false;
    private boolean actionCompleted = false;
    public CondaEnvironmentFinder() {
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setSize(600, 320);
        setLocationRelativeTo(null); // Center on screen

        JPanel contentPane = new JPanel();
        contentPane.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        setContentPane(contentPane);

        // Initialize components
        condaPathField = new JTextField(20);
        condaPathField.setEditable(false);
        condaEnvComboBox = new JComboBox<>();
        JButton selectCondaPathButton = new JButton("Select Conda Path");
        JButton addCondaEnvButton = new JButton("New Ultrack Env.");

        // Layout
        setLayout(new BoxLayout(contentPane, BoxLayout.PAGE_AXIS));
        {
            final JPanel pnNorth = new JPanel();
            pnNorth.setLayout(new BorderLayout());
            JTextArea textArea = new JTextArea("Select the path to the conda executable and then the enviroment where is installed Ultrack. If you are not sure where conda is installed, you can try to find it by running 'which conda' in a terminal.");
            textArea.setEditable(false);
            textArea.setLineWrap(true);
            textArea.setOpaque(false);
            textArea.setBorder(BorderFactory.createEmptyBorder());
            textArea.setFont(UIManager.getFont("Label.font"));
            pnNorth.add(textArea, BorderLayout.CENTER);
            add(pnNorth);
        }
        {
            final JPanel pnCenter = new JPanel();
            GridBagLayout gridBagLayout = new GridBagLayout();
            pnCenter.setLayout(gridBagLayout);
            GridBagConstraints c = new GridBagConstraints();
            // First row
            c.fill = GridBagConstraints.HORIZONTAL;
            c.weightx = 1;
            c.gridx = 0;
            c.gridy = 0;
            JLabel lbPath = new JLabel("Conda Path");
            gridBagLayout.setConstraints(lbPath, c);
            // Second row
            c.gridy++;
            c.gridx = 0;
            c.gridwidth = 4;
            pnCenter.add(lbPath);
            gridBagLayout.setConstraints(condaPathField, c);
            pnCenter.add(condaPathField);
            c.gridx = 4;
            c.gridwidth = 2;
            c.weightx = 0;
            gridBagLayout.setConstraints(selectCondaPathButton, c);
            pnCenter.add(selectCondaPathButton);
            // Third row
            c.gridy++;
            c.gridx = 0;
            JLabel lbEnv = new JLabel("Conda Environment");
            gridBagLayout.setConstraints(lbEnv, c);
            pnCenter.add(lbEnv);
            // Fourth row
            c.gridy++;
            c.gridx = 0;
            c.gridwidth = 5;
            c.weightx = 1;
            gridBagLayout.setConstraints(condaEnvComboBox, c);
            pnCenter.add(condaEnvComboBox);

            c.gridx = 5;
            c.gridwidth = 1;
            c.weightx = 0;
            gridBagLayout.setConstraints(addCondaEnvButton, c);
            pnCenter.add(addCondaEnvButton);

            add(pnCenter);
        }
        {
            final JPanel pnSouth = new JPanel();
            pnSouth.setLayout(new GridLayout(1, 2, 5, 5));
            final JButton btnOk = new JButton("OK");
            btnOk.addActionListener(e -> {
                if (condaEnvComboBox.getSelectedItem() == null || condaEnvComboBox.getSelectedItem().toString().isEmpty()) {
                    JOptionPane.showMessageDialog(CondaEnvironmentFinder.this, "No conda environment was selected.", "Error", JOptionPane.ERROR_MESSAGE);
                } else {
                    String path = ((CondaEnvironment) condaEnvComboBox.getSelectedItem()).getPath();

                    if (checkIfCanExecute(path + File.separator + "bin" + File.separator + "ultrack")
                            || checkIfCanExecute(path + File.separator + "Scripts" + File.separator + "ultrack.exe")) {
                        JOptionPane.showMessageDialog(CondaEnvironmentFinder.this, "Ultrack was found in the selected conda environment.", "Success", JOptionPane.INFORMATION_MESSAGE);
                    } else {
                        JOptionPane.showMessageDialog(CondaEnvironmentFinder.this, "Ultrack was not found in the selected conda environment.", "Error", JOptionPane.ERROR_MESSAGE);
                        return;
                    }

                    Preferences prefs = Preferences.userNodeForPackage(CondaEnvironmentFinder.class);
                    prefs.put("condaEnv", ((CondaEnvironment) condaEnvComboBox.getSelectedItem()).getPath());
                    synchronized (lock) {
                        actionCompleted = true;
                        lock.notify(); // Notify the waiting thread
                    }
                    dispose();
                }
            });
            final JButton btnCancel = new JButton("Cancel");
            btnCancel.addActionListener(e -> {
                this.dispose();
                synchronized (lock) {
                    cancelled = true;
                    actionCompleted = true;
                    lock.notify(); // Notify the waiting thread
                }
            });

            addWindowListener(new WindowAdapter() {
                @Override
                public void windowClosed(WindowEvent e) {
                    btnCancel.doClick();
                }
            });


            pnSouth.add(btnOk);
            pnSouth.add(btnCancel);
            add(pnSouth);
        }

        // Add action listener to button
        selectCondaPathButton.addActionListener(e -> {
            JFileChooser chooser = new JFileChooser();
            chooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
            int result = chooser.showOpenDialog(CondaEnvironmentFinder.this);
            if (result == JFileChooser.APPROVE_OPTION) {
                File selectedFile = chooser.getSelectedFile();
                if (selectedFile.isDirectory()) {
                    selectedFile = new File(selectedFile.getAbsolutePath() + File.separator + "conda");
                }
                condaPathField.setText(selectedFile.getAbsolutePath());
                updateCondaEnvironments(selectedFile);
                Preferences prefs = Preferences.userNodeForPackage(CondaEnvironmentFinder.class);
                prefs.put("condaPath", selectedFile.getAbsolutePath());
                try {
                    prefs.flush();
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        });

        addCondaEnvButton.addActionListener(e -> {
            String condaPath = condaPathField.getText();
            if (condaPath.isEmpty()) {
                JOptionPane.showMessageDialog(CondaEnvironmentFinder.this, "Please select the conda path first.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            String uuid = java.util.UUID.randomUUID().toString();
            uuid = uuid.substring(0, 4);
            String envName = "ultrack_env_" + uuid;

            String createCommand = condaPath + " create -y -n " + envName + " -c conda-forge python=3.10 git";
            String installUltrackCommand = condaPath + " run  -n " + envName + " --no-capture-output python -m pip install ultrack[api]@git+https://github.com/royerlab/ultrack";

            CompletableFuture<Integer> future = new CommandExecutor((Frame) this.getOwner(), createCommand).executeCommand();

            future.thenAccept(exitCode -> {
                if (exitCode != 0) {
                    JOptionPane.showMessageDialog(CondaEnvironmentFinder.this, "Failed to create Ultrack environment.", "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                CompletableFuture<Integer> future2 = new CommandExecutor((Frame) this.getOwner(), installUltrackCommand).executeCommand();

                future2.thenAccept(exitCodeInstall -> {
                    if (exitCodeInstall != 0) {
                        JOptionPane.showMessageDialog(CondaEnvironmentFinder.this, "Failed to install Ultrack in the new environment.", "Error", JOptionPane.ERROR_MESSAGE);
                        System.out.println("Exit code: " + exitCodeInstall);
                        return;
                    } else {
                        JOptionPane.showMessageDialog(CondaEnvironmentFinder.this, "Ultrack was successfully installed in the new environment.", "Success", JOptionPane.INFORMATION_MESSAGE);
                    }

                    updateCondaEnvironments(new File(condaPath));

                    // find envName in the list of environments
                    for (int i = 0; i < condaEnvComboBox.getItemCount(); i++) {
                        CondaEnvironment env = condaEnvComboBox.getItemAt(i);
                        if (env.getPath().contains(envName)) {
                            condaEnvComboBox.setSelectedItem(env);
                            break;
                        }
                    }
                });
            });
        });

        String condaPath = getCurrentCondaPath();

        if (condaPath != null) {
            condaPathField.setText(condaPath);
            updateCondaEnvironments(new File(condaPath));
        }
    }

    public static void main(String[] args) {
        try {
            String path = openDialogToFindUltrack();
            System.out.println("Selected conda environment: " + path);
        } catch (InterruptedException e) {
            JOptionPane.showMessageDialog(null, "Failed to find Ultrack: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    public static String getCurrentCondaPath() {
        Preferences prefs = Preferences.userNodeForPackage(CondaEnvironmentFinder.class);
        return prefs.get("condaPath", null);
    }

    public static String getCurrentCondaEnv() {
        Preferences prefs = Preferences.userNodeForPackage(CondaEnvironmentFinder.class);
        return prefs.get("condaEnv", null);
    }


    private static boolean checkIfCanExecute(String program) {
        return new File(program).canExecute();
    }

    private static String tryFindingConda() {
        String command;
        String osName = System.getProperty("os.name").toLowerCase();
        if (osName.contains("win")) {
            command = "where conda";
        } else {
            command = "which conda";
        }

        // Execute the command

        try {
            Process process = Runtime.getRuntime().exec(command);
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line = reader.readLine();
            int exitVal = process.waitFor();

            if (exitVal == 0 && line != null && !line.isEmpty()) {
                return line; // This should be the path to Conda
            } else {
                return null; // Conda not found
            }
        } catch (IOException | InterruptedException e) {
            return null;
        }
    }

    public static String openDialogToFindUltrack() throws InterruptedException {
        boolean ultrackAvailable = checkIfCanExecute("ultrack");

        boolean openCondaEnvironmentSelector = true;
        if (ultrackAvailable) {
            int result = JOptionPane.showConfirmDialog(null, "Ultrack was found in your current PATH environment. Do you want to select another conda environment to execute?", "Ultrack Available", JOptionPane.YES_NO_OPTION);
            if (result == JOptionPane.NO_OPTION) {
                openCondaEnvironmentSelector = false;
            }
        }

        final CondaEnvironmentFinder[] frame = {null};

        if (openCondaEnvironmentSelector) {
            String condaPath = getCurrentCondaPath();
            if (condaPath == null) {
                boolean condaAvailable = checkIfCanExecute("conda");
                if (condaAvailable) {
                    condaPath = tryFindingConda();
                    Preferences prefs = Preferences.userNodeForPackage(CondaEnvironmentFinder.class);
                    prefs.put("condaPath", condaPath);
                } else {
                    JOptionPane.showMessageDialog(null, "Conda was not found in your current PATH environment. Please select the conda path manually.", "Conda Not Found", JOptionPane.ERROR_MESSAGE);
                }
            }


            System.out.println("Opening Conda Environment Finder");
            frame[0] = new CondaEnvironmentFinder();
            frame[0].pack();
            frame[0].setVisible(true);
            boolean sucessfull = frame[0].execute();
            if (!sucessfull) {
                return null;
            }

            CondaEnvironment selectedEnv = (CondaEnvironment) frame[0].condaEnvComboBox.getSelectedItem();

            return selectedEnv.getPath();
        } else {
            return null;
        }
    }

    public static String getUltrackPath() throws InterruptedException {
        String condaPath = getCurrentCondaEnv();
        ArrayList<String> possibleUltrackPaths = new ArrayList<>();
        possibleUltrackPaths.add("ultrack");
        if (condaPath != null) {
            possibleUltrackPaths.add(condaPath + File.separator + "bin" + File.separator + "ultrack");
            possibleUltrackPaths.add(condaPath + File.separator + "Scripts" + File.separator + "ultrack.exe");
        }

        for (String path : possibleUltrackPaths) {
            if (checkIfCanExecute(path)) {
                return path;
            }
        }

        condaPath = CondaEnvironmentFinder.openDialogToFindUltrack();
        if (condaPath == null) {
            return null;
        }
        return CondaEnvironmentFinder.getUltrackPath();
    }

    private void buildGUI() {

    }

    private boolean execute() {
        synchronized (lock) {
            while (!actionCompleted) {
                try {
                    lock.wait(); // Wait until the action is completed
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    System.out.println("Interrupted!");
                }
            }
        }
        return !cancelled;
    }

    private void updateCondaEnvironments(File condaPath) {
        try {
            String command = condaPath.getAbsolutePath() + " env list";
            Process process = Runtime.getRuntime().exec(command);
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));

            String line;
            ArrayList<CondaEnvironment> environments = new ArrayList<>();
            while ((line = reader.readLine()) != null) {
                if (!line.startsWith("#")) {
                    String[] parts = line.split("\\s+");
                    if (parts.length > 0) {
                        if (!parts[parts.length - 1].isEmpty()) {
                            String path = parts[parts.length - 1];
                            environments.add(new CondaEnvironment(path));
                        }
                    }
                }
            }

            // Update the JComboBox with the list of environments
            condaEnvComboBox.removeAllItems();
            for (CondaEnvironment env : environments) {
                condaEnvComboBox.addItem(env);
            }

            String previousCondaEnv = getCurrentCondaEnv();
            if (previousCondaEnv != null) {
                condaEnvComboBox.setSelectedItem(new CondaEnvironment(previousCondaEnv));
            }

        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "Failed to list Conda environments.", "Error", JOptionPane.ERROR_MESSAGE);
            ex.printStackTrace();
        }
    }
}
