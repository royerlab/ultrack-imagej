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
import java.awt.*;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.prefs.Preferences;

public class CondaEnvironmentFinder extends JDialog {

    private final Object lock = new Object();
    private boolean actionCompleted = false;

    public static void main(String[] args) {
        try {
            String path = openDialogToFindUltrack();
            System.out.println("Selected conda environment: " + path);
        } catch (InterruptedException e) {
            JOptionPane.showMessageDialog(null, "Failed to find Ultrack: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    private final JTextField condaPathField;
    private final JComboBox<CondaEnvironment> condaEnvComboBox;

    public static String getCurrentCondaPath() {
        Preferences prefs = Preferences.userNodeForPackage(CondaEnvironmentFinder.class);
        return prefs.get("condaPath", null);
    }

    public static String getCurrentCondaEnv() {
        Preferences prefs = Preferences.userNodeForPackage(CondaEnvironmentFinder.class);
        return prefs.get("condaEnv", null);
    }

    public CondaEnvironmentFinder() {
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setSize(600, 220);
        setLocationRelativeTo(null); // Center on screen

        JPanel contentPane = new JPanel();
        contentPane.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        setContentPane(contentPane);

        // Initialize components
        condaPathField = new JTextField(20);
        condaPathField.setEditable(false);
        condaEnvComboBox = new JComboBox<>();
        JButton selectCondaPathButton = new JButton("Select Conda Path");

        // Layout
        setLayout(new BorderLayout(5, 5));
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
            add(pnNorth, BorderLayout.NORTH);
        }
        {
            final JPanel pnCenter = new JPanel();
            GridBagLayout gridBagLayout = new GridBagLayout();
            pnCenter.setLayout(gridBagLayout);
            GridBagConstraints c = new GridBagConstraints();
            c.fill = GridBagConstraints.HORIZONTAL;
            c.gridx = 0;
            c.gridy = 0;
            c.weightx = 1;
            JLabel lbPath = new JLabel("Conda Path");
            gridBagLayout.setConstraints(lbPath, c);
            c.gridy++;
            pnCenter.add(lbPath);
            c.gridx = 0;
            gridBagLayout.setConstraints(condaPathField, c);
            pnCenter.add(condaPathField);
            c.gridx = 1;
            gridBagLayout.setConstraints(selectCondaPathButton, c);
            pnCenter.add(selectCondaPathButton);
            c.gridx = 0;
            c.gridy++;
            JLabel lbEnv = new JLabel("Conda Environment");
            gridBagLayout.setConstraints(lbEnv, c);
            pnCenter.add(lbEnv);
            c.gridy++;
            c.gridx = 0;
            c.gridwidth = 2;
            gridBagLayout.setConstraints(condaEnvComboBox, c);
            pnCenter.add(condaEnvComboBox);
            add(pnCenter, BorderLayout.CENTER);
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

                    if (checkIfCanExecute(path + "/bin/ultrack")) {
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
                    actionCompleted = true;
                    lock.notify(); // Notify the waiting thread
                }
            });
            pnSouth.add(btnOk);
            pnSouth.add(btnCancel);
            add(pnSouth, BorderLayout.SOUTH);
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

        String condaPath = getCurrentCondaPath();

        if (condaPath != null) {
            condaPathField.setText(condaPath);
            updateCondaEnvironments(new File(condaPath));
        }
    }

    /**
     * Check if a given path can be executed.
     *
     * @param path the path to check.
     * @param strict if true, it will try to execute the path. If false, it will check if the path is executable.
     * @return true if the path can be executed, false otherwise.
     */
    private static boolean checkIfCanExecute(String path, boolean strict) {
        if (!strict) {
            return new File(path).canExecute();
        } else {
            try {
                ProcessBuilder builder = new ProcessBuilder(path);
                Process process = builder.start();
                int exitCode = process.waitFor();
                return exitCode == 0;
            } catch (IOException | InterruptedException e) {
                return false;
            }
        }
    }


    /**
     * Check if a given path can be executed. It will not try to execute the path.
     *
     * @param path the path to check.
     * @return true if the path can be executed, false otherwise.
     * @see #checkIfCanExecute(String, boolean) for a strict check if the path can be executed.
     */
    private static boolean checkIfCanExecute(String path) {
        return checkIfCanExecute(path, false);
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
                    condaPath = "conda";
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
            frame[0].execute();

            CondaEnvironment selectedEnv = (CondaEnvironment) frame[0].condaEnvComboBox.getSelectedItem();

            return selectedEnv.getPath();
        } else {
            return null;
        }
    }

    private void execute() {
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
    }

    public static String getUltrackPath(String condaPath) {
        if (condaPath == null) {
            return "ultrack";
        } else {
            return condaPath + "/bin/ultrack";
        }
    }

    public static String getUltrackPath() throws InterruptedException {
        String condaPath = getCurrentCondaEnv();
        String ultrackPath = getUltrackPath(condaPath);
        if (checkIfCanExecute(ultrackPath)) {
            return ultrackPath;
        } else {
            condaPath = CondaEnvironmentFinder.openDialogToFindUltrack();
            ultrackPath = CondaEnvironmentFinder.getUltrackPath(condaPath);
            return ultrackPath;
        }
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
