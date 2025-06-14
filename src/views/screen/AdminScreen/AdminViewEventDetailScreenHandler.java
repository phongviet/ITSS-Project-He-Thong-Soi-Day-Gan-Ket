package views.screen.AdminScreen;

import controller.AdminApprovalController;
import controller.UserController;
import controller.event.EventController;
import entity.events.Event;
import entity.events.EventParticipantDetails;
import entity.users.Admin;
import entity.users.Volunteer;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.text.Text;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

public class AdminViewEventDetailScreenHandler implements Initializable {

    @FXML private Label eventTitleLabel;
    @FXML private Text titleText;
    @FXML private Text organizerText;
    @FXML private Text startDateText;
    @FXML private Text endDateText;
    @FXML private Text maxParticipantsText;
    @FXML private Text emergencyLevelText;
    @FXML private Text statusText;
    @FXML private TextArea descriptionTextArea;
    @FXML private ListView<String> requiredSkillsListView;
    @FXML private Button approveButton;
    @FXML private Button rejectButton;
    @FXML private Label participantsCountLabel;
    @FXML private TableView<EventParticipantDetails> participantsTableView;
    @FXML private TableColumn<EventParticipantDetails, String> participantNameColumn;
    @FXML private TableColumn<EventParticipantDetails, String> participantStatusColumn;
    @FXML private TableColumn<EventParticipantDetails, String> participantHoursColumn;
    @FXML private TableColumn<EventParticipantDetails, String> participantRatingColumn;
    @FXML private Button backButton;

    private Stage stage;
    private Admin admin; // Admin hiện tại
    private Event event; // Sự kiện được xem chi tiết

    private EventController eventController;
    private UserController userController;
    private AdminApprovalController adminApprovalController;
    private SimpleDateFormat dateFormatter = new SimpleDateFormat("dd/MM/yyyy 'at' HH:mm");

    public AdminViewEventDetailScreenHandler() {
        this.eventController = new EventController();
        this.userController = new UserController();
        this.adminApprovalController = new AdminApprovalController();
    }

    public void setStage(Stage stage) {
        this.stage = stage;
    }

    public void setAdmin(Admin admin) {
        this.admin = admin;
    }

    public void setEvent(Event event) {
        this.event = event;
        populateEventDetails();
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        // Set up table columns for participants
        setupParticipantsTable();
    }

    private void setupParticipantsTable() {
        participantNameColumn.setCellValueFactory(cellData ->
            new SimpleStringProperty(cellData.getValue().getVolunteerUsername()));

        participantStatusColumn.setCellValueFactory(cellData ->
            new SimpleStringProperty(cellData.getValue().getVolunteerParticipationStatus()));

        participantHoursColumn.setCellValueFactory(cellData -> {
            Integer hours = cellData.getValue().getHoursParticipated();
            return new SimpleStringProperty(hours != null ? hours.toString() + " hrs" : "N/A");
        });

        participantRatingColumn.setCellValueFactory(cellData -> {
            Integer rating = cellData.getValue().getRatingByOrg();
            return new SimpleStringProperty(rating != null ? rating.toString() + "/5" : "N/A");
        });
    }

    private void populateEventDetails() {
        if (event == null) {
            eventTitleLabel.setText("Event Details Not Found");
            return;
        }

        // Set basic event information
        eventTitleLabel.setText("Details for: " + event.getTitle());
        titleText.setText(event.getTitle());
        organizerText.setText(event.getOrganizer() != null ? event.getOrganizer() : "N/A");
        startDateText.setText(event.getStartDate() != null ? dateFormatter.format(event.getStartDate()) : "N/A");
        endDateText.setText(event.getEndDate() != null ? dateFormatter.format(event.getEndDate()) : "N/A");

        Integer maxParticipants = event.getMaxParticipantNumber();
        maxParticipantsText.setText(maxParticipants != null ? (maxParticipants != 0 ? String.valueOf(maxParticipants) : "Unlimited") : "N/A");
        emergencyLevelText.setText(event.getEmergencyLevel() != null ? event.getEmergencyLevel() : "N/A");
        statusText.setText(event.getStatus() != null ? event.getStatus() : "Pending");
        descriptionTextArea.setText(event.getDescription() != null ? event.getDescription() : "No description available.");

        // Set skills list
        if (event.getRequiredSkills() != null && !event.getRequiredSkills().isEmpty()) {
            requiredSkillsListView.setItems(FXCollections.observableArrayList(event.getRequiredSkills()));
        } else {
            requiredSkillsListView.setItems(FXCollections.observableArrayList("No specific skills required."));
        }

        // Update status of approval buttons based on event status
        boolean isPending = "Pending".equalsIgnoreCase(event.getStatus());
        approveButton.setVisible(isPending);
        rejectButton.setVisible(isPending);

        // Load participants information
        loadParticipantsData();
    }

    private void loadParticipantsData() {
        try {
            // This method is likely returning List<Volunteer> instead of List<EventParticipantDetails>
            // We need to convert between types or modify the controller method
            List<Volunteer> participants = userController.getEventParticipants(event.getEventId());

            if (participants != null && !participants.isEmpty()) {
                // Create EventParticipantDetails objects from Volunteer objects
                List<EventParticipantDetails> participantDetails = new ArrayList<>();
                for (Volunteer volunteer : participants) {
                    // Create a simple EventParticipantDetails with available information
                    EventParticipantDetails detail = new EventParticipantDetails();
                    detail.setEventId(event.getEventId());
                    detail.setTitle(event.getTitle());
                    detail.setStartDate(event.getStartDate());
                    detail.setEndDate(event.getEndDate());
                    detail.setEventStatus(event.getStatus());
                    detail.setVolunteerUsername(volunteer.getUsername());
                    // Other fields would need to be populated if available

                    participantDetails.add(detail);
                }

                participantsTableView.setItems(FXCollections.observableArrayList(participantDetails));

                // Safe null check for maxParticipantNumber
                Integer maxParticipants = event.getMaxParticipantNumber();
                if (maxParticipants != null) {
                    participantsCountLabel.setText(String.format("%d of %d maximum participants",
                        participants.size(), maxParticipants));
                } else {
                    participantsCountLabel.setText(String.format("%d participants (maximum not specified)",
                        participants.size()));
                }
            } else {
                participantsCountLabel.setText("No participants registered yet");
                participantsTableView.setItems(FXCollections.observableArrayList());
            }
        } catch (Exception e) {
            System.err.println("Error loading participants data: " + e.getMessage());
            participantsCountLabel.setText("Error loading participants data");
        }
    }

    @FXML
    public void handleApproveEvent() {
        if (admin == null || event == null) {
            return;
        }

        try {
            boolean success = adminApprovalController.approveEvent(event.getEventId());
            if (success) {
                // Update event status locally
                event.setStatus("Approved");
                statusText.setText("Approved");

                // Hide approval buttons
                approveButton.setVisible(false);
                rejectButton.setVisible(false);
            }
        } catch (Exception e) {
            System.err.println("Error approving event: " + e.getMessage());
        }
    }

    @FXML
    public void handleRejectEvent() {
        if (admin == null || event == null) {
            return;
        }

        try {
            boolean success = adminApprovalController.rejectEvent(event.getEventId());
            if (success) {
                // Update event status locally
                event.setStatus("Rejected");
                statusText.setText("Rejected");

                // Hide approval buttons
                approveButton.setVisible(false);
                rejectButton.setVisible(false);
            }
        } catch (Exception e) {
            System.err.println("Error rejecting event: " + e.getMessage());
        }
    }

    @FXML
    public void handleBackToList() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/fxml/AdminScreen/EventManagementScreen.fxml"));
            Parent root = loader.load();

            EventManagementScreenHandler controller = loader.getController();
            controller.setStage(this.stage);
            controller.setAdmin(this.admin);

            Scene scene = new Scene(root);
            this.stage.setScene(scene);
            this.stage.setTitle("Event Management");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
