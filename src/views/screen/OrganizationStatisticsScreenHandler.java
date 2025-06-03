package views.screen;

import controller.event.EventController;
import entity.users.VolunteerOrganization;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

public class OrganizationStatisticsScreenHandler implements Initializable {

    @FXML private Label totalEventsLabel;

    @FXML private Button backButton;

    private Stage stage;
    private VolunteerOrganization organization;
    private EventController eventController;

    public OrganizationStatisticsScreenHandler() {
        this.eventController = new EventController();
    }

    public void setStage(Stage stage) {
        this.stage = stage;
    }

    public void setOrganization(VolunteerOrganization organization) {
        this.organization = organization;
        if (this.organization != null) {
            loadStatistics();
        }
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        // Nothing specific to initialize here for the simplified version
    }

    private void loadStatistics() {
        if (organization == null || eventController == null) {
            System.err.println("Organization or EventController not initialized for statistics.");
            totalEventsLabel.setText("N/A");
            return;
        }

        String organizerId = organization.getUsername();

        // TODO: Replace with actual call to eventController.getTotalEventCountByOrganizer(organizerId)
        // int totalEvents = eventController.getTotalEventCountByOrganizer(organizerId);
        // totalEventsLabel.setText(String.valueOf(totalEvents));
        
        // Using a placeholder value for now
        // totalEventsLabel.setText("7"); // Example: Display 7 total events

        // Actual call to EventController
        try {
            int totalEvents = eventController.getTotalEventCountByOrganizer(organizerId);
            totalEventsLabel.setText(String.valueOf(totalEvents));
        } catch (Exception e) {
            System.err.println("Error loading total event count: " + e.getMessage());
            e.printStackTrace();
            totalEventsLabel.setText("Error");
        }
    }

    @FXML
    public void handleBackToDashboard() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/fxml/OrganizationScreen/VolunteerOrgMainScreen.fxml"));
            Parent root = loader.load();
            VolunteerOrgMainScreenHandler controller = loader.getController();
            controller.setStage(stage);
            controller.setOrganization(organization);
            Scene scene = new Scene(root);
            stage.setScene(scene);
            stage.setTitle("Volunteer Organization Dashboard");
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
            // Consider showing an error alert to the user
        }
    }

    // handleExportReport can be removed if not planned for this simplified view
    /*
    @FXML
    public void handleExportReport() {
        System.out.println("Export Report button clicked (simplified view).");
    }
    */
} 