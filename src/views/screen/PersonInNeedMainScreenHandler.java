package views.screen;

import entity.users.PersonInNeed;
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

public class PersonInNeedMainScreenHandler implements Initializable {

    @FXML
    private Button requestHelpButton;

    @FXML
    private Button myRequestsButton;

    @FXML
    private Label statusMessage;

    private Stage stage;
    private PersonInNeed personInNeed;

    public PersonInNeedMainScreenHandler(Stage stage, PersonInNeed personInNeed) {
        this.stage = stage;
        this.personInNeed = personInNeed;
    }

    // Default constructor needed for FXML loader
    public PersonInNeedMainScreenHandler() {
    }

    // Setter methods
    public void setStage(Stage stage) {
        this.stage = stage;
    }

    public void setPersonInNeed(PersonInNeed personInNeed) {
        this.personInNeed = personInNeed;
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
    public void handleRequestHelp() {
        // This will be implemented later
        statusMessage.setText("Request Help feature will be implemented soon.");
    }

    @FXML
    public void handleViewMyRequests() {
        // This will be implemented later
        statusMessage.setText("View My Requests feature will be implemented soon.");
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
