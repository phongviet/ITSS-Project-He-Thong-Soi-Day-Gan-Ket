package views.screen;

import controller.event.EventController;
import entity.events.Event;
import entity.users.VolunteerOrganization;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.TextFieldTableCell; // For editing hours
import javafx.stage.Stage;
import javafx.util.Callback;
import javafx.util.converter.IntegerStringConverter;

import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

// Wrapper class for displaying participant details and their ratings
class ParticipantRating {
    private StringProperty username;
    private StringProperty fullName;
    private IntegerProperty hoursParticipated;
    private ObjectProperty<Integer> ratingByOrg;

    public ParticipantRating(String username, String fullName, Integer hours, Integer rating) {
        this.username = new SimpleStringProperty(username);
        this.fullName = new SimpleStringProperty(fullName);
        this.hoursParticipated = new SimpleIntegerProperty(hours != null ? hours : 0);
        this.ratingByOrg = new SimpleObjectProperty<>(rating);
    }

    public String getUsername() { return username.get(); }
    public StringProperty usernameProperty() { return username; }
    public String getFullName() { return fullName.get(); }
    public StringProperty fullNameProperty() { return fullName; }
    public int getHoursParticipated() { return hoursParticipated.get(); }
    public IntegerProperty hoursParticipatedProperty() { return hoursParticipated; }
    public void setHoursParticipated(int hours) { this.hoursParticipated.set(hours);}
    public Integer getRatingByOrg() { return ratingByOrg.get(); }
    public ObjectProperty<Integer> ratingByOrgProperty() { return ratingByOrg; }
    public void setRatingByOrg(Integer rating) { this.ratingByOrg.set(rating); }
}

public class RateEventVolunteersScreenHandler implements Initializable {

    @FXML private Label titleLabel;
    @FXML private Label eventDetailsLabel;
    @FXML private TableView<ParticipantRating> volunteersTableView;
    @FXML private TableColumn<ParticipantRating, String> usernameColumn;
    @FXML private TableColumn<ParticipantRating, String> fullNameColumn;
    @FXML private TableColumn<ParticipantRating, Integer> hoursParticipatedColumn;
    @FXML private TableColumn<ParticipantRating, Integer> ratingColumn;
    @FXML private Label statusMessageLabel;
    @FXML private Button saveRatingsButton;
    @FXML private Button backButton;

    private Stage stage;
    private VolunteerOrganization organization;
    private Event eventToRate;
    private EventController eventController;
    private ObservableList<ParticipantRating> participantsList = FXCollections.observableArrayList();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        this.eventController = new EventController();
        setupTableColumns();
        volunteersTableView.setItems(participantsList);
        volunteersTableView.setEditable(true); // Allow editing in table
        statusMessageLabel.setText("");
    }

    public void setStage(Stage stage) {
        this.stage = stage;
    }

    public void setOrganization(VolunteerOrganization organization) {
        this.organization = organization;
    }

    public void setEventToRate(Event event) {
        this.eventToRate = event;
        titleLabel.setText("Rate Volunteers for Event: " + event.getTitle());
        eventDetailsLabel.setText("Event ID: " + event.getEventId() + " - Status: " + event.getStatus());
        loadParticipantData();
    }

    private void setupTableColumns() {
        usernameColumn.setCellValueFactory(cellData -> cellData.getValue().usernameProperty());
        fullNameColumn.setCellValueFactory(cellData -> cellData.getValue().fullNameProperty());
        
        hoursParticipatedColumn.setCellValueFactory(cellData -> cellData.getValue().hoursParticipatedProperty().asObject());
        hoursParticipatedColumn.setCellFactory(TextFieldTableCell.forTableColumn(new IntegerStringConverter()));
        hoursParticipatedColumn.setOnEditCommit(
            (TableColumn.CellEditEvent<ParticipantRating, Integer> event) -> {
                ParticipantRating participant = event.getRowValue();
                participant.setHoursParticipated(event.getNewValue() != null ? event.getNewValue() : participant.getHoursParticipated());
            }
        );

        ratingColumn.setCellValueFactory(cellData -> cellData.getValue().ratingByOrgProperty());
        ratingColumn.setCellFactory(TextFieldTableCell.forTableColumn(new IntegerStringConverter()));
        ratingColumn.setOnEditCommit(
            (TableColumn.CellEditEvent<ParticipantRating, Integer> event) -> {
                ParticipantRating participant = event.getRowValue();
                Integer newValue = event.getNewValue();
                Integer oldValue = event.getOldValue(); 

                if (newValue != null) {
                    if (newValue >= 1 && newValue <= 10) {
                        participant.setRatingByOrg(newValue);
                        statusMessageLabel.setText(""); 
                    } else {
                        statusMessageLabel.setText("Invalid rating for " + participant.getUsername() + ": " + newValue + ". Must be 1-10.");
                        participant.setRatingByOrg(oldValue);
                        volunteersTableView.getItems().set(event.getTablePosition().getRow(), participant);
                        volunteersTableView.refresh(); 
                    }
                } else {
                    participant.setRatingByOrg(null);
                    statusMessageLabel.setText("Rating for " + participant.getUsername() + " cleared (no rating).");
                }
            }
        );
    }

    private void loadParticipantData() {
        if (eventToRate == null) return;
        // TODO: Need a method in EventController to get participants with their full names and existing ratings/hours
        // For now, using a placeholder or assuming EventController.getParticipantDetailsForEvent returns what's needed
        // This list should be of a type that includes username, fullName, hours, rating.
        // List<SomeParticipantDetailClass> details = eventController.getParticipantDetailsForEvent(eventToRate.getEventId());
        
        // Placeholder data - replace with actual data loading
        // List<entity.events.EventParticipantDetails> details = eventController.getParticipantDetailsForEvent(eventToRate.getEventId());
        // if (details != null) {
        //     participantsList.setAll(details.stream()
        //         .map(d -> new ParticipantRating(d.getVolunteerUsername(), "N/A", d.getHoursParticipated(), d.getRatingByOrg()))
        //         .collect(Collectors.toList()));
        // }
        statusMessageLabel.setText("INFO: Actual data loading for participants needs implementation in EventController.");
        // Example with dummy data for now if getParticipantDetailsForEvent is not ready
        // participantsList.add(new ParticipantRating("tnv1", "Volunteer One", 10, 4));
        // participantsList.add(new ParticipantRating("tnv2", "Volunteer Two", 8, 0));

        // We will use `eventController.getEventParticipants(eventId)` which returns List<Volunteer>
        // and then for each volunteer, try to find their entry in EventParticipants table for hours/rating.
        // This is inefficient. It's better if EventController has a method like getDetailedParticipants(eventId).
        // For now, I will assume getParticipantDetailsForEvent exists as per previous plans
        List<entity.events.EventParticipantDetails> details = eventController.getParticipantDetailsForEvent(eventToRate.getEventId());
         if (details != null) {
             participantsList.setAll(details.stream()
                 .map(d -> new ParticipantRating(d.getVolunteerUsername(), d.getVolunteerFullName(), d.getHoursParticipated(), d.getRatingByOrg()))
                 .collect(Collectors.toList()));
             if (participantsList.isEmpty()) {
                statusMessageLabel.setText("No registered participants found for this event to rate.");
             } else {
                statusMessageLabel.setText(""); // Clear if data loaded
             }
         } else {
            statusMessageLabel.setText("Could not load participant details.");
         }
    }

    @FXML
    private void handleSaveRatings() {
        if (eventToRate == null || participantsList.isEmpty()) {
            statusMessageLabel.setText("No participants to rate or event not loaded.");
            return;
        }
        boolean allSuccess = true;
        int updatedCount = 0;
        for (ParticipantRating participant : participantsList) {
            boolean success = eventController.updateEventParticipantDetails(
                eventToRate.getEventId(), 
                participant.getUsername(), 
                participant.getHoursParticipated(), 
                participant.getRatingByOrg()
            );
            
            if (participant.getRatingByOrg() != null) {
                eventController.updateVolunteerRating(participant.getUsername(), participant.getRatingByOrg());
            }

            if (success) {
                updatedCount++;
            } else {
                allSuccess = false;
            }
        }
        if (allSuccess) {
            statusMessageLabel.setText(updatedCount + " participant(s) rated/updated successfully!");
        } else {
            statusMessageLabel.setText("Some ratings/updates failed. " + updatedCount + " succeeded.");
        }
    }

    @FXML
    private void handleBackToEventList() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/fxml/OrganizationScreen/VolunteerOrgViewEventListScreen.fxml"));
            Parent root = loader.load();
            VolunteerOrgViewEventListScreenHandler controller = loader.getController();
            controller.setStage(stage);
            controller.setOrganization(organization);
            // controller.loadEventData(); // Ensure the list is refreshed if needed

            Scene scene = new Scene(root);
            stage.setScene(scene);
            stage.setTitle("My Events List");
            stage.show();
        } catch (IOException e) {
            statusMessageLabel.setText("Error loading event list screen: " + e.getMessage());
            e.printStackTrace();
        }
    }
} 