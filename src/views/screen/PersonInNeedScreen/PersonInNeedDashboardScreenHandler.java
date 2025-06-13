package views.screen.PersonInNeedScreen;

import entity.users.PersonInNeed;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.stage.Stage;

import java.io.IOException;

public class PersonInNeedDashboardScreenHandler {

    @FXML private Label dashboardTitleLabel;
    @FXML private Button requestHelpButton;
    @FXML private Button myRequestsButton;
    @FXML private Button logoutButton;
    @FXML private Label infoLabel; // For messages like "Request Help feature..."

    private Stage stage;
    private PersonInNeed currentUser;

    public PersonInNeedDashboardScreenHandler() {
        // Default constructor for FXML
    }

    public void setStage(Stage stage) {
        this.stage = stage;
    }

    public void setCurrentUser(PersonInNeed user) {
        this.currentUser = user;
        updateDashboardTitle();
        if (currentUser == null) {
            // Disable buttons if no user is logged in
            requestHelpButton.setDisable(true);
            myRequestsButton.setDisable(true);
            infoLabel.setText("Please log in to access features.");
        } else {
            requestHelpButton.setDisable(false);
            myRequestsButton.setDisable(false);
            // Clear the placeholder message from the FXML if it was set there
            // Or set a welcome message.
             infoLabel.setText("Welcome, " + currentUser.getName() + "!"); // Assumes getName() exists
             infoLabel.setStyle("-fx-text-fill: #2c3e50;");
        }
    }

    @FXML
    public void initialize() {
        // The infoLabel text can be set here if it's static or cleared
        // infoLabel.setText("Request Help feature will be implemented soon."); 
        // For now, let setCurrentUser handle the infoLabel text based on user state.
        infoLabel.setText(""); 
    }

    private void updateDashboardTitle() {
        if (currentUser != null && currentUser.getName() != null) {
            dashboardTitleLabel.setText(currentUser.getName() + "'s Dashboard");
        } else {
            dashboardTitleLabel.setText("Person In Need Dashboard");
        }
    }

    @FXML
    private void handleRequestHelp() {
        if (currentUser == null) {
            infoLabel.setText("Please log in to request help.");
            infoLabel.setStyle("-fx-text-fill: red;");
            return;
        }
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/fxml/PersonInNeedScreen/PersonInNeedCreateRequestScreen.fxml"));
            Parent root = loader.load();
            PersonInNeedCreateRequestScreenHandler controller = loader.getController();
            controller.setStage(stage);
            controller.setCurrentUser(currentUser);
            Scene scene = new Scene(root);
            stage.setScene(scene);
            stage.setTitle("Create Help Request");
        } catch (IOException e) {
            e.printStackTrace();
            infoLabel.setText("Error loading 'Request Help' screen: " + e.getMessage());
            infoLabel.setStyle("-fx-text-fill: red;");
        }
    }

    @FXML
    private void handleMyRequests() {
        if (currentUser == null) {
            infoLabel.setText("Please log in to view your requests.");
            infoLabel.setStyle("-fx-text-fill: red;");
            return;
        }
        try {
            java.net.URL fxmlUrl = getClass().getResource("/views/fxml/PersonInNeedScreen/PersonInNeedRequestListScreen.fxml");
            if (fxmlUrl == null) {
                infoLabel.setText("Error: FXML file for 'My Requests' not found at path /views/fxml/PersonInNeedScreen/PersonInNeedRequestListScreen.fxml");
                infoLabel.setStyle("-fx-text-fill: red;");
                System.err.println("FXML Resource not found: /views/fxml/PersonInNeedScreen/PersonInNeedRequestListScreen.fxml");
                return;
            }
            FXMLLoader loader = new FXMLLoader(fxmlUrl);
            Parent root = loader.load();
            PersonInNeedRequestListScreenHandler controller = loader.getController();
            controller.setStage(stage);
            controller.setCurrentUser(currentUser);
            Scene scene = new Scene(root);
            stage.setScene(scene);
            stage.setTitle("My Help Requests");
        } catch (IOException e) {
            e.printStackTrace();
            // Keep the original error display but now we know fxmlUrl was not null if we reach here
            infoLabel.setText("Error loading 'My Requests' screen (FXML found but failed to load): " + e.getMessage());
            infoLabel.setStyle("-fx-text-fill: red;");
        }
    }

    @FXML
    private void handleLogout() {
        System.out.println("Logout button clicked. User: " + (currentUser != null ? currentUser.getName() : "Guest"));
        this.currentUser = null; 
        // Navigate to Login Screen (assuming LoginScreen.fxml exists in /views/fxml/)
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/fxml/LoginScreen.fxml"));
            Parent root = loader.load();
            // If LoginScreen has a controller that needs the stage:
            // LoginScreenHandler controller = loader.getController();
            // controller.setStage(stage);
            Scene scene = new Scene(root);
            stage.setScene(scene);
            stage.setTitle("Login");
        } catch (IOException e) {
            e.printStackTrace();
            infoLabel.setText("Error loading login screen: " + e.getMessage());
            infoLabel.setStyle("-fx-text-fill: red;");
        }
    }
} 