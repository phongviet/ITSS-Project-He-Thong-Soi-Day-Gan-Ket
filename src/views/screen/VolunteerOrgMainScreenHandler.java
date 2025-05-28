package views.screen;

import controller.event.EventController;
import entity.users.VolunteerOrganization;
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

public class VolunteerOrgMainScreenHandler implements Initializable {

    @FXML
    private Button registerEventButton;

    @FXML
    private Button viewEventsButton;

    @FXML
    private Label statusMessage;

    private Stage stage;
    private VolunteerOrganization organization;
    private EventController eventController;

    public VolunteerOrgMainScreenHandler(Stage stage, VolunteerOrganization organization) {
        this.stage = stage;
        this.organization = organization;
        this.eventController = new EventController();
    }

    // Add a default constructor
    public VolunteerOrgMainScreenHandler() {
        this.eventController = new EventController();
    }

    // Add setter methods
    public void setStage(Stage stage) {
        this.stage = stage;
    }

    public void setOrganization(VolunteerOrganization organization) {
        this.organization = organization;
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
    public void handleRegisterEvent() {
        try {
            // Load the event registration screen
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/fxml/OrganizationScreen/VolunteerOrgRegisterEventScreen.fxml"));
            Parent root = loader.load();

            // Get the controller and pass the organization data
            VolunteerOrgRegisterEventScreenHandler controller = loader.getController();
            controller.setStage(stage);
            controller.setOrganization(organization);

            // Set the scene
            Scene scene = new Scene(root);
            stage.setScene(scene);
            stage.setTitle("Register New Event");
            stage.show();
        } catch (IOException e) {
            statusMessage.setText("Error loading registration screen: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    public void handleViewEvents() {
        try {
            // Load the event list screen
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/fxml/OrganizationScreen/VolunteerOrgViewEventListScreen.fxml"));
            Parent root = loader.load();

            // Get the controller and pass the organization data
            VolunteerOrgViewEventListScreenHandler controller = loader.getController();
            controller.setStage(stage);
            controller.setOrganization(organization);

            // Set the scene
            Scene scene = new Scene(root);
            stage.setScene(scene);
            stage.setTitle("My Events List");
            stage.show();
        } catch (IOException e) {
            statusMessage.setText("Error loading events list screen: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    public void handleLogout() {
        try {
            // Load the login FXML
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/fxml/LogInScreen.fxml"));
            Parent root = loader.load();

            // Set the scene
            Scene scene = new Scene(root);
            stage.setScene(scene);
            stage.setTitle("Login");
            stage.show();
        } catch (IOException e) {
            statusMessage.setText("Error logging out: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
