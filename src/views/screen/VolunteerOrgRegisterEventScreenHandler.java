package views.screen;

import controller.event.EventController;
import entity.events.Event;
import entity.users.VolunteerOrganization;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

public class VolunteerOrgRegisterEventScreenHandler implements Initializable {

    @FXML
    private TextField eventTitle;

    @FXML
    private TextArea eventDescription;

    @FXML
    private ComboBox<String> supportType;

    @FXML
    private TextField maxParticipants;

    @FXML
    private ComboBox<String> emergencyLevel;

    @FXML
    private TextField startDay, startMonth, startYear;

    @FXML
    private TextField endDay, endMonth, endYear;

    @FXML
    private Label statusMessage;

    @FXML
    private VBox eventRegistrationForm;

    private Stage stage;
    private VolunteerOrganization organization;
    private EventController eventController;

    public VolunteerOrgRegisterEventScreenHandler() {
        this.eventController = new EventController();
    }

    public VolunteerOrgRegisterEventScreenHandler(Stage stage, VolunteerOrganization organization) {
        this.stage = stage;
        this.organization = organization;
        this.eventController = new EventController();
    }

    public void setStage(Stage stage) {
        this.stage = stage;
    }

    public void setOrganization(VolunteerOrganization organization) {
        this.organization = organization;
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        // Initialize ComboBox values
        supportType.getItems().addAll("Food", "Shelter", "Medical", "Education", "Financial", "Other");
        emergencyLevel.getItems().addAll("Low", "Medium", "High", "Critical");

        // Clear any previous status message
        statusMessage.setText("");
    }

    @FXML
    public void handleSubmit() {
        try {
            // Validate inputs
            if (eventTitle.getText().isEmpty()) {
                statusMessage.setText("Event title is required.");
                return;
            }

            if (eventDescription.getText().isEmpty()) {
                statusMessage.setText("Event description is required.");
                return;
            }

            if (supportType.getValue() == null) {
                statusMessage.setText("Support type must be selected.");
                return;
            }

            if (emergencyLevel.getValue() == null) {
                statusMessage.setText("Emergency level must be selected.");
                return;
            }

            // Parse numeric values with validation
            int maxParticipantsNum;
            try {
                maxParticipantsNum = Integer.parseInt(maxParticipants.getText());
                if (maxParticipantsNum <= 0) {
                    statusMessage.setText("Maximum participants must be a positive number.");
                    return;
                }
            } catch (NumberFormatException e) {
                statusMessage.setText("Maximum participants must be a valid number.");
                return;
            }

            // Validate and parse dates
            int startDayVal, startMonthVal, startYearVal, endDayVal, endMonthVal, endYearVal;
            try {
                startDayVal = Integer.parseInt(startDay.getText());
                startMonthVal = Integer.parseInt(startMonth.getText());
                startYearVal = Integer.parseInt(startYear.getText());
                endDayVal = Integer.parseInt(endDay.getText());
                endMonthVal = Integer.parseInt(endMonth.getText());
                endYearVal = Integer.parseInt(endYear.getText());

                // Basic validation - could be more sophisticated
                if (startDayVal < 1 || startDayVal > 31 || startMonthVal < 1 || startMonthVal > 12 ||
                        endDayVal < 1 || endDayVal > 31 || endMonthVal < 1 || endMonthVal > 12) {
                    statusMessage.setText("Please enter valid dates.");
                    return;
                }
            } catch (NumberFormatException e) {
                statusMessage.setText("All date fields must be valid numbers.");
                return;
            }

            // Create a new Event object
            Event event = new Event();
            event.setTitle(eventTitle.getText());
            event.setDescription(eventDescription.getText());
            event.setSupportType(supportType.getValue());
            event.setMaxParticipantNumber(maxParticipantsNum);
            event.setEmergencyLevel(emergencyLevel.getValue());

            // Set dates
            event.setStartDay(startDayVal);
            event.setStartMonth(startMonthVal);
            event.setStartYear(startYearVal);
            event.setEndDay(endDayVal);
            event.setEndMonth(endMonthVal);
            event.setEndYear(endYearVal);

            // Set the organization as the organizer
            if (organization != null) {
                event.setOrganizer(organization.getUsername());
            }

            // Save event using the controller
            boolean success = eventController.registerEvent(event, organization);

            if (success) {
                statusMessage.setText("Event registered successfully!");
                // Navigate back to main screen after successful registration
                handleBack();
            } else {
                statusMessage.setText("Failed to register event. Please try again.");
            }

        } catch (Exception e) {
            statusMessage.setText("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    public void handleCancel() {
        // Go back to the main screen
        handleBack();
    }

    @FXML
    public void handleBack() {
        try {
            // Load the main organization screen
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/fxml/OrganizationScreen/VolunteerOrgMainScreen.fxml"));
            Parent root = loader.load();

            // Get the controller and pass the organization data
            VolunteerOrgMainScreenHandler controller = loader.getController();
            controller.setStage(stage);
            controller.setOrganization(organization);

            // Set the scene
            Scene scene = new Scene(root);
            stage.setScene(scene);
            stage.setTitle("Volunteer Organization Dashboard");

            // Show success message on the main screen if needed
            controller.setStatusMessage("Event registration " +
                (statusMessage.getText().contains("successfully") ? "successful!" : "canceled."));

            stage.show();
        } catch (IOException e) {
            statusMessage.setText("Error navigating back: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void clearForm() {
        eventTitle.setText("");
        eventDescription.setText("");
        supportType.setValue(null);
        maxParticipants.setText("");
        emergencyLevel.setValue(null);
        startDay.setText("");
        startMonth.setText("");
        startYear.setText("");
        endDay.setText("");
        endMonth.setText("");
        endYear.setText("");
        statusMessage.setText("");
    }

    public void setStatusMessage(String message) {
        statusMessage.setText(message);
    }
}
