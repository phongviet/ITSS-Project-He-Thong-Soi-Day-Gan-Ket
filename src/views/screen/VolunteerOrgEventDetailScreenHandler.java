package views.screen;

import controller.event.EventController;
import entity.events.Event;
import entity.events.EventParticipantDetails;
import entity.users.VolunteerOrganization;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.stage.Stage;
import javafx.util.converter.IntegerStringConverter;

import java.io.IOException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.ResourceBundle;

public class VolunteerOrgEventDetailScreenHandler implements Initializable {

    @FXML private Label titleLabel;
    @FXML private Label startDateLabel;
    @FXML private Label endDateLabel;
    @FXML private Label statusLabel;
    @FXML private Label emergencyLevelLabel;
    @FXML private Label requestIdLabel;
    @FXML private Label descriptionLabel;
    @FXML private Label eventMaxParticipantsLabel;
    @FXML private Label eventOrganizerLabel;
    @FXML private TableView<EventParticipantDetails> volunteersTableView;
    @FXML private TableColumn<EventParticipantDetails, String> volunteerNameColumn;
    @FXML private TableColumn<EventParticipantDetails, Integer> hoursColumn;
    @FXML private TableColumn<EventParticipantDetails, Integer> ratingColumn;
    @FXML private Button backButton;
    @FXML private Button saveVolunteerDataButton;
    @FXML private Label volunteerStatusMessageLabel;

    private Stage stage;
    private VolunteerOrganization organization;
    private Event event;
    private EventController eventController;
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("dd/MM/yyyy");
    private ObservableList<EventParticipantDetails> participantDetailsList = FXCollections.observableArrayList();

    public VolunteerOrgEventDetailScreenHandler() {
        this.eventController = new EventController();
    }

    public void setStage(Stage stage) {
        this.stage = stage;
    }

    public void setOrganization(VolunteerOrganization org) {
        this.organization = org;
    }

    public void setEvent(Event event) {
        this.event = event;
        populateEventDetails();
        loadVolunteers();
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        volunteerNameColumn.setCellValueFactory(new PropertyValueFactory<>("volunteerFullName"));

        hoursColumn.setCellValueFactory(new PropertyValueFactory<>("hoursParticipated"));
        hoursColumn.setCellFactory(TextFieldTableCell.forTableColumn(new IntegerStringConverter()));
        hoursColumn.setOnEditCommit(
            (TableColumn.CellEditEvent<EventParticipantDetails, Integer> t) -> {
                EventParticipantDetails participant = t.getTableView().getItems().get(t.getTablePosition().getRow());
                participant.setHoursParticipated(t.getNewValue());
            });

        ratingColumn.setCellValueFactory(new PropertyValueFactory<>("ratingByOrg"));
        ratingColumn.setCellFactory(TextFieldTableCell.forTableColumn(new IntegerStringConverter()));
        ratingColumn.setOnEditCommit(
            (TableColumn.CellEditEvent<EventParticipantDetails, Integer> t) -> {
                EventParticipantDetails participant = t.getTableView().getItems().get(t.getTablePosition().getRow());
                Integer newValue = t.getNewValue();
                if (newValue != null && (newValue < 1 || newValue > 10)) {
                    if(volunteerStatusMessageLabel != null) volunteerStatusMessageLabel.setText("Rating for " + participant.getVolunteerFullName() + " must be between 1 and 10.");
                    participant.setRatingByOrg(t.getOldValue()); 
                    volunteersTableView.refresh(); 
                } else {
                    participant.setRatingByOrg(newValue);
                    if(volunteerStatusMessageLabel != null) volunteerStatusMessageLabel.setText(""); 
                }
            });
        
        volunteersTableView.setEditable(true);
        volunteersTableView.setItems(participantDetailsList);
        if (volunteerStatusMessageLabel != null) volunteerStatusMessageLabel.setText("");
    }

    private void populateEventDetails() {
        if (event == null) return;

        titleLabel.setText(event.getTitle() != null ? event.getTitle() : "N/A");
        if (event.getStartDate() != null) {
            startDateLabel.setText(DATE_FORMAT.format(event.getStartDate()));
        } else {
            startDateLabel.setText("N/A");
        }
        if (event.getEndDate() != null) {
            endDateLabel.setText(DATE_FORMAT.format(event.getEndDate()));
        } else {
            endDateLabel.setText("N/A");
        }

        statusLabel.setText(event.getStatus() != null ? event.getStatus() : "N/A");
        emergencyLevelLabel.setText(event.getEmergencyLevel() != null ? event.getEmergencyLevel() : "N/A");
        descriptionLabel.setText(event.getDescription() != null ? event.getDescription() : "N/A");
        
        Integer maxNum = event.getMaxParticipantNumber();
        if (maxNum != null && maxNum > 0) {
            eventMaxParticipantsLabel.setText(String.valueOf(maxNum));
        } else if (maxNum != null && maxNum == 0) {
            eventMaxParticipantsLabel.setText("Unlimited/Not specified");
        } else {
            eventMaxParticipantsLabel.setText("N/A");
        }
       
        if (event.getOrganizer() != null) {
            eventOrganizerLabel.setText(event.getOrganizer());
        } else {
            eventOrganizerLabel.setText("N/A");
        }

        if (event.getRequestId() != null) {
            requestIdLabel.setText(event.getRequestId().toString());
        } else {
            requestIdLabel.setText("N/A");
        }
    }

    private void loadVolunteers() {
        if (event != null && eventController != null) {
            List<EventParticipantDetails> participants = eventController.getParticipantDetailsForEvent(event.getEventId());
            participantDetailsList.setAll(participants);
            if (participants.isEmpty()) {
                volunteersTableView.setPlaceholder(new Label("No volunteers have participated or are registered for this event."));
            }
        } else {
            participantDetailsList.clear();
            volunteersTableView.setPlaceholder(new Label("Event data not available to load volunteers."));
        }
    }
    
    @FXML
    private void handleSaveVolunteerData() {
        if (event == null) {
            if(volunteerStatusMessageLabel != null) volunteerStatusMessageLabel.setText("No event loaded.");
            return;
        }
        if (participantDetailsList.isEmpty()) {
            if(volunteerStatusMessageLabel != null) volunteerStatusMessageLabel.setText("No volunteer data to save.");
            return;
        }

        int successCount = 0;
        int failCount = 0;
        StringBuilder errors = new StringBuilder();

        for (EventParticipantDetails participant : participantDetailsList) {
            Integer rating = participant.getRatingByOrg();
            if (rating != null && (rating < 1 || rating > 10)) {
                errors.append("Invalid rating for ").append(participant.getVolunteerFullName()).append(" (must be 1-10). Data not saved.\n");
                failCount++;
                continue; 
            }

            boolean detailsUpdated = eventController.updateEventParticipantDetails(event.getEventId(), participant.getVolunteerUsername(), participant.getHoursParticipated(), rating);

            if(detailsUpdated) {
                if (rating != null) { 
                    eventController.recalculateVolunteerAverageRating(participant.getVolunteerUsername());
                }
                successCount++;
            } else {
                errors.append("Failed to save details for ").append(participant.getVolunteerFullName()).append(".\n");
                failCount++;
            }
        }

        if (failCount > 0) {
            if(volunteerStatusMessageLabel != null) volunteerStatusMessageLabel.setText("Saved " + successCount + ". Failed for " + failCount + ".\n" + errors.toString());
        } else {
            if(volunteerStatusMessageLabel != null) volunteerStatusMessageLabel.setText("All volunteer data saved successfully for " + successCount + " participants!");
        }
        loadVolunteers(); 
    }

    @FXML
    public void handleBack() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/fxml/OrganizationScreen/VolunteerOrgViewEventListScreen.fxml"));
            Parent root = loader.load();
            VolunteerOrgViewEventListScreenHandler controller = loader.getController();
            controller.setStage(stage);
            controller.setOrganization(organization);
            Scene scene = new Scene(root, 1024, 768);
            stage.setScene(scene);
            stage.setTitle("My Events List");
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
            if(volunteerStatusMessageLabel != null) volunteerStatusMessageLabel.setText("Error going back: " + e.getMessage());
        }
    }
}
