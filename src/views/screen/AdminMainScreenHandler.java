package views.screen;

import entity.users.Admin;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

public class AdminMainScreenHandler implements Initializable {

    @FXML
    private Button userManagementButton;

    @FXML
    private Button eventManagementButton;

    @FXML
    private Button statisticsButton;

    @FXML
    private Label statusMessage;

    private Stage stage;
    private Admin admin;

    public AdminMainScreenHandler(Stage stage, Admin admin) {
        this.stage = stage;
        this.admin = admin;
    }

    // Default constructor needed for FXML loader
    public AdminMainScreenHandler() {
    }

    // Setter methods
    public void setStage(Stage stage) {
        this.stage = stage;
    }

    public void setAdmin(Admin admin) {
        this.admin = admin;
    }

    public void setStatusMessage(String message) {
        statusMessage.setText(message);
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        // Clear any previous status message
        statusMessage.setText("");
    }

    @FXML
    public void handleUserManagement() {
        // This will be implemented later
        statusMessage.setText("User Management feature will be implemented soon.");
    }

    @FXML
    public void handleEventManagement() {
        // This will be implemented later
        statusMessage.setText("Event Management feature will be implemented soon.");
    }

    @FXML
    public void handleViewStatistics() {
        // This will be implemented later
        statusMessage.setText("Statistics View feature will be implemented soon.");
    }

    @FXML
    public void handleLogout() {
        try {
            // Load the login FXML
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/fxml/LogInScreen.fxml"));
            Parent root = loader.load();

            // Set the scene
            Scene scene = new Scene(root, 1024, 768);
            stage.setScene(scene);
            stage.setTitle("Login");
            stage.show();
        } catch (IOException e) {
            statusMessage.setText("Error logging out: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
