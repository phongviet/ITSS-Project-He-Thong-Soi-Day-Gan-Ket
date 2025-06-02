package views.screen;

import controller.event.EventController;
import controller.verification.VerificationController;
import entity.events.Event;
import entity.users.Volunteer;
import entity.users.VolunteerOrganization;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;
import javafx.scene.text.Text;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ResourceBundle;

public class VolunteerSuggestedEventDetailScreenHandler implements Initializable {

    @FXML private Label eventTitleHeaderLabel;
    @FXML private Text titleText;
    @FXML private Text organizerText;
    @FXML private Text startDateText;
    @FXML private Text endDateText;
    @FXML private Text eventStatusText; // Trạng thái chung của sự kiện
    @FXML private Text maxParticipantsText;
    @FXML private Text emergencyLevelText;
    @FXML private TextArea descriptionTextArea;
    @FXML private ListView<String> requiredSkillsListView;
    @FXML private Label infoMessageLabel; // Để hiển thị thông báo (nếu có)
    @FXML private Button backButton;

    private Stage stage;
    private Volunteer volunteer; // TNV đang xem
    private Event eventToDisplay; // Event được truyền vào
    private EventController eventController;
    private VerificationController verificationController;
    private SimpleDateFormat dateFormatter = new SimpleDateFormat("dd/MM/yyyy"); // Chỉ ngày

    public VolunteerSuggestedEventDetailScreenHandler() {
        this.eventController = new EventController();
        this.verificationController = new VerificationController();
    }

    public void setStage(Stage stage) {
        this.stage = stage;
    }

    public void setVolunteer(Volunteer volunteer) {
        this.volunteer = volunteer;
    }

    public void setEventToDisplay(Event event) {
        this.eventToDisplay = event;
        populateDetails();
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        infoMessageLabel.setText("");
    }

    private void populateDetails() {
        if (eventToDisplay == null) {
            eventTitleHeaderLabel.setText("Event Not Found");
            clearFields();
            return;
        }

        // Lấy thông tin Event đầy đủ (bao gồm skills) nếu cần.
        // Giả sử eventToDisplay đã có skills được load từ EventController.getSuggestedEventsForVolunteer
        Event fullEvent = eventController.getEventById(eventToDisplay.getEventId()); // Đảm bảo load skills

        if (fullEvent == null) {
            eventTitleHeaderLabel.setText("Error Loading Event Details");
            clearFields();
            return;
        }

        eventTitleHeaderLabel.setText("Details for: " + fullEvent.getTitle());
        titleText.setText(fullEvent.getTitle());

        if (fullEvent.getOrganizer() != null) {
            VolunteerOrganization org = verificationController.getVolunteerOrganization(fullEvent.getOrganizer());
            organizerText.setText(org != null && org.getOrganizationName() != null ? org.getOrganizationName() : "Unknown");
        } else {
            organizerText.setText("N/A");
        }

        startDateText.setText(fullEvent.getStartDate() != null ? dateFormatter.format(fullEvent.getStartDate()) : "N/A");
        endDateText.setText(fullEvent.getEndDate() != null ? dateFormatter.format(fullEvent.getEndDate()) : "N/A");
        eventStatusText.setText(fullEvent.getStatus() != null ? fullEvent.getStatus() : "N/A");
        Integer maxParticipants = fullEvent.getMaxParticipantNumber();
    	maxParticipantsText.setText(maxParticipants != null && maxParticipants != 0 ? String.valueOf(maxParticipants) : "Not specified");
        emergencyLevelText.setText(fullEvent.getEmergencyLevel() != null ? fullEvent.getEmergencyLevel() : "N/A");
        descriptionTextArea.setText(fullEvent.getDescription() != null ? fullEvent.getDescription() : "No description provided.");

        if (fullEvent.getRequiredSkills() != null && !fullEvent.getRequiredSkills().isEmpty()) {
            requiredSkillsListView.setItems(FXCollections.observableArrayList(fullEvent.getRequiredSkills()));
        } else {
            requiredSkillsListView.setItems(FXCollections.observableArrayList("No specific skills required."));
        }
    }

    private void clearFields() {
        titleText.setText("");
        organizerText.setText("");
        startDateText.setText("");
        endDateText.setText("");
        eventStatusText.setText("");
        maxParticipantsText.setText("");
        emergencyLevelText.setText("");
        descriptionTextArea.setText("");
        requiredSkillsListView.setItems(FXCollections.observableArrayList());
    }

    @FXML
    public void handleBackToSuggestedList() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/fxml/VolunteerScreen/VolunteerSuggestedEventsScreen.fxml"));
            Parent root = loader.load();

            VolunteerSuggestedEventsScreenHandler suggestedListController = loader.getController();
            suggestedListController.setStage(this.stage);
            suggestedListController.setVolunteer(this.volunteer);

            Scene scene = new Scene(root);
            this.stage.setScene(scene);
            this.stage.setTitle("Suggested Event Invitations");
        } catch (IOException e) {
            infoMessageLabel.setText("Error returning to list: " + e.getMessage());
            infoMessageLabel.setStyle("-fx-text-fill: red;");
            e.printStackTrace();
        }
    }
}