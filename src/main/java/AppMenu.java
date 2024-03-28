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

//    this.setStyle("-fx-font: 12px \"Arial\"");
//        getMenus().addAll(fileMenu, envMenu, helpMenu);
    }
}
