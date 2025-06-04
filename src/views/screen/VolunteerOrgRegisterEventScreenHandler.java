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
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.ResourceBundle;
import entity.requests.HelpRequest;

public class VolunteerOrgRegisterEventScreenHandler implements Initializable {
    private HelpRequest helpRequest;

    @FXML
    private TextField eventTitle;

    @FXML
    private TextArea eventDescription;

    @FXML
    private TextField maxParticipants;

    @FXML
    private ComboBox<String> emergencyLevel;

    @FXML
    private DatePicker startDatePicker;

    @FXML
    private DatePicker endDatePicker;

    @FXML
    private CheckBox communicationSkillCheckbox;

    @FXML
    private CheckBox firstAidSkillCheckbox;

    @FXML
    private CheckBox educationSkillCheckbox;

    @FXML
    private CheckBox cookingSkillCheckbox;

    @FXML
    private CheckBox drivingSkillCheckbox;

    @FXML
    private CheckBox fundraisingSkillCheckbox;

    @FXML
    private Label statusMessage;

    @FXML
    private VBox eventRegistrationForm;

    private Stage stage;
    private VolunteerOrganization organization;
    private EventController eventController;

    public void setHelpRequest(HelpRequest helpRequest) {
        this.helpRequest = helpRequest;
    }

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
        emergencyLevel.getItems().addAll("Low", "Medium", "High", "Critical");

        // Setup default date values
        startDatePicker.setValue(LocalDate.now());
        endDatePicker.setValue(LocalDate.now().plusDays(1));

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

            // Validate dates
            LocalDate startLocalDate = startDatePicker.getValue();
            LocalDate endLocalDate = endDatePicker.getValue();

            if (startLocalDate == null) {
                statusMessage.setText("Start date is required.");
                return;
            }

            if (endLocalDate == null) {
                statusMessage.setText("End date is required.");
                return;
            }

            if (startLocalDate.isAfter(endLocalDate)) {
                statusMessage.setText("Start date cannot be after end date.");
                return;
            }

            // Convert LocalDate to java.util.Date
            Date startDate = Date.from(startLocalDate.atStartOfDay(ZoneId.systemDefault()).toInstant());
            Date endDate = Date.from(endLocalDate.atStartOfDay(ZoneId.systemDefault()).toInstant());

            // Create a new Event object
            Event event = new Event();
            event.setTitle(eventTitle.getText());
            event.setDescription(eventDescription.getText());
            event.setMaxParticipantNumber(maxParticipantsNum);
            event.setEmergencyLevel(emergencyLevel.getValue());

            // Set dates
            event.setStartDate(startDate);
            event.setEndDate(endDate);

            // Process required skills from checkboxes
            ArrayList<String> skills = new ArrayList<>();
            if (communicationSkillCheckbox.isSelected()) skills.add("Communication");
            if (firstAidSkillCheckbox.isSelected()) skills.add("First Aid");
            if (educationSkillCheckbox.isSelected()) skills.add("Education");
            if (cookingSkillCheckbox.isSelected()) skills.add("Cooking");
            if (drivingSkillCheckbox.isSelected()) skills.add("Driving");
            if (fundraisingSkillCheckbox.isSelected()) skills.add("Fundraising");

            event.setRequiredSkills(skills);

            // Set the organization as the organizer
            if (organization != null) {
                event.setOrganizer(organization.getUsername());
            }

            // Save event using the controller
            boolean success = eventController.registerEvent(event, organization);

            if (success) {
                statusMessage.setText("Event registered successfully!");

                // Nếu đến từ HelpRequest, cập nhật trạng thái HelpRequest thành "closed"
                // if (helpRequest != null) {
                //     eventController.updateHelpRequestStatus(helpRequest.getRequestId(), "Closed");
                // }

                // Thực hiện quay về dashboard
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

            Scene scene = new Scene(root);
            stage.setScene(scene);
            stage.setTitle("Volunteer Organization Dashboard");
            stage.show();
        } catch (IOException e) {
            statusMessage.setText("Error returning to dashboard: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
