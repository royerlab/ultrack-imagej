import javafx.scene.control.Alert;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;

public class AppMenu extends MenuBar {

    private String setCondaEnv() {
        String currentCondaEnv = CondaEnvironmentFinder.getCurrentCondaEnv();
        if (currentCondaEnv == null) {
            currentCondaEnv = "No conda env selected";
        }
        return currentCondaEnv;
    }

    public AppMenu() {
        Menu fileMenu = new Menu("File");
        Menu envMenu = new Menu("Environment");
        Menu helpMenu = new Menu("Help");

        // File menu
        {
            // Initialization
            MenuItem exitMenu = new MenuItem("Exit");

            // Addition
            fileMenu.getItems().add(exitMenu);

            // Action
            exitMenu.setOnAction(actionEvent -> System.exit(0));
        }

        // Environment menu
        {
            // Initialization
            MenuItem currentConda = new MenuItem();
            MenuItem selectCondaPathMenu = new MenuItem("Select Conda Path");

            String currentCondaEnv = setCondaEnv();
            currentConda.setText(currentCondaEnv);
            currentConda.setStyle("-fx-text-fill: gray;");

            // Addition
            envMenu.getItems().add(currentConda);
            envMenu.getItems().add(selectCondaPathMenu);

            // Action
            selectCondaPathMenu.setOnAction(actionEvent -> {
                try {
                    String path = CondaEnvironmentFinder.openDialogToFindUltrack();
                    String updatedEnv = setCondaEnv();
                    currentConda.setText(updatedEnv);
                    System.out.println("Selected conda environment: " + path);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            });

            currentConda.setOnAction(actionEvent -> {
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Current Conda Environment");
                alert.setHeaderText("Current Conda Environment");
                alert.setContentText(currentCondaEnv + " is selected as the current conda environment located at " + CondaEnvironmentFinder.getCurrentCondaPath());
                alert.showAndWait();
            });
        }

        {
            // Help menu
            helpMenu.getItems().add(new MenuItem("About"));
        }

    this.setStyle("-fx-font: 12px \"Arial\"");
        getMenus().addAll(fileMenu, envMenu, helpMenu);
    }
}
