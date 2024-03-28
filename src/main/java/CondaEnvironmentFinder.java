import javafx.scene.control.Alert;

import javax.swing.*;
import java.awt.*;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.prefs.Preferences;

public class CondaEnvironmentFinder extends JFrame {

    public static void main(String[] args) {
        try {
            String path = openDialogToFindUltrack();
            System.out.println("Selected conda environment: " + path);
        } catch (InterruptedException e) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setHeaderText("Failed to find Ultrack");
            alert.setContentText(e.getMessage());
            alert.showAndWait();
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

    public CondaEnvironmentFinder(CountDownLatch latch) {
        super("Conda Environment Selector");
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
                    latch.countDown();
                    dispose();
                }
            });
            final JButton btnCancel = new JButton("Cancel");
            btnCancel.addActionListener(e -> {this.dispose(); latch.countDown();});
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

    private static boolean checkIfCanExecute(String path) {
        try {
            ProcessBuilder builder = new ProcessBuilder(path);
            Process process = builder.start();
            int exitCode = process.waitFor();
            return exitCode == 0;
        } catch (IOException | InterruptedException e) {
            return false;
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
                    condaPath = "conda";
                    Preferences prefs = Preferences.userNodeForPackage(CondaEnvironmentFinder.class);
                    prefs.put("condaPath", condaPath);
                } else {
                    JOptionPane.showMessageDialog(null, "Conda was not found in your current PATH environment. Please select the conda path manually.", "Conda Not Found", JOptionPane.ERROR_MESSAGE);
                }
            }

            final CountDownLatch latch = new CountDownLatch(1);

            SwingUtilities.invokeLater(() -> {
                frame[0] = new CondaEnvironmentFinder(latch);
                frame[0].setVisible(true);
            });

            latch.await();

            CondaEnvironment selectedEnv = (CondaEnvironment) frame[0].condaEnvComboBox.getSelectedItem();

            return selectedEnv.getPath();
        } else {
            return null;
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
