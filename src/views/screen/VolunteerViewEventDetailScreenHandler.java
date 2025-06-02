package views.screen;

import controller.event.EventController;
import entity.events.Event; // Cần import Event
import entity.events.EventParticipantDetails;
import entity.users.Volunteer;
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
import java.util.List;
import java.util.ResourceBundle;

public class VolunteerViewEventDetailScreenHandler implements Initializable {

    @FXML private Label eventTitleLabel;
    @FXML private Text titleText;
    @FXML private Text organizerText;
    @FXML private Text startDateText;
    @FXML private Text endDateText;
    @FXML private Text maxParticipantsText;
    @FXML private Text emergencyLevelText;
    @FXML private TextArea descriptionTextArea;
    @FXML private ListView<String> requiredSkillsListView;
    @FXML private Text myStatusText;
    @FXML private Text myHoursText;
    @FXML private Text myOrgRatingText;
    @FXML private Button cancelParticipationButton;
    @FXML private Button backButton; // fx:id cho nút back

    private Stage stage;
    private Volunteer volunteer; // TNV hiện tại
    private EventParticipantDetails eventDetails; // Thông tin chi tiết đã được tổng hợp
    private EventController eventController;
    private SimpleDateFormat dateFormatter = new SimpleDateFormat("dd/MM/yyyy 'at' HH:mm"); // Định dạng chi tiết hơn

    // Constructor mặc định
    public VolunteerViewEventDetailScreenHandler() {
        this.eventController = new EventController();
    }

    // Setter để truyền dữ liệu
    public void setStage(Stage stage) {
        this.stage = stage;
    }

    public void setVolunteer(Volunteer volunteer) {
        this.volunteer = volunteer;
    }

    public void setEventDetails(EventParticipantDetails eventDetails) {
        this.eventDetails = eventDetails;
        populateEventDetails();
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        // Khởi tạo ban đầu nếu cần
    }

    private void populateEventDetails() {
        if (eventDetails == null) {
            eventTitleLabel.setText("Event Details Not Found");
            // Có thể ẩn các trường khác hoặc hiển thị thông báo lỗi
            return;
        }

        eventTitleLabel.setText("Details for: " + eventDetails.getTitle());
        titleText.setText(eventDetails.getTitle());
        
        // Lấy tên Organizer - Cần phương thức trong EventController hoặc truyền từ trước
        // Tạm thời để trống hoặc dùng username của organizer nếu có trong EventParticipantDetails
        // Hoặc bạn có thể sửa EventParticipantDetails để chứa Event object đầy đủ
        // Giả sử Event object có trong EventParticipantDetails hoặc được lấy riêng
        // String organizerUsername = eventController.getEventById(eventDetails.getEventId()).getOrganizer(); // Ví dụ
        // organizerText.setText(eventController.getOrganizerName(organizerUsername)); // Cần getOrganizerName
        organizerText.setText(eventDetails.getOrganizerName() != null ? eventDetails.getOrganizerName() : "N/A");


        startDateText.setText(eventDetails.getStartDate() != null ? dateFormatter.format(eventDetails.getStartDate()) : "N/A");
        endDateText.setText(eventDetails.getEndDate() != null ? dateFormatter.format(eventDetails.getEndDate()) : "N/A");
        
        // Lấy thông tin đầy đủ của Event từ eventId nếu EventParticipantDetails không đủ
        Event fullEvent = eventController.getEventById(eventDetails.getEventId()); // Cần phương thức này
        if (fullEvent != null) {
        	Integer maxParticipants = fullEvent.getMaxParticipantNumber();
        	maxParticipantsText.setText(maxParticipants != null && maxParticipants != 0 ? String.valueOf(maxParticipants) : "N/A");
            emergencyLevelText.setText(fullEvent.getEmergencyLevel() != null ? fullEvent.getEmergencyLevel() : "N/A");
            descriptionTextArea.setText(fullEvent.getDescription() != null ? fullEvent.getDescription() : "No description available.");
            if (fullEvent.getRequiredSkills() != null && !fullEvent.getRequiredSkills().isEmpty()) {
                requiredSkillsListView.setItems(FXCollections.observableArrayList(fullEvent.getRequiredSkills()));
            } else {
                requiredSkillsListView.setItems(FXCollections.observableArrayList("No specific skills required."));
            }
        } else {
             maxParticipantsText.setText("N/A");
             emergencyLevelText.setText("N/A");
             descriptionTextArea.setText("Event data could not be fully loaded.");
             requiredSkillsListView.setItems(FXCollections.observableArrayList());
        }


        myStatusText.setText(eventDetails.getVolunteerParticipationStatus() != null ? eventDetails.getVolunteerParticipationStatus() : "N/A");
        myHoursText.setText(eventDetails.getHoursParticipated() != null ? eventDetails.getHoursParticipated().toString() + " hours" : "N/A");
        myOrgRatingText.setText(eventDetails.getRatingByOrg() != null ? eventDetails.getRatingByOrg().toString() + "/5" : "N/A");

        
    }

    @FXML
    public void handleCancelParticipation() {
        if (volunteer == null || eventDetails == null) {
            // Hiển thị lỗi
            return;
        }
        // TODO: Gọi phương thức trong EventController để hủy đăng ký tham gia của TNV
        // boolean success = eventController.cancelVolunteerParticipation(volunteer.getUsername(), eventDetails.getEventId());
        // if (success) {
        //     myStatusText.setText("Participation Canceled");
        //     cancelParticipationButton.setVisible(false);
        //     // Có thể hiển thị thông báo thành công
        // } else {
        //     // Hiển thị thông báo lỗi
        // }
        System.out.println("Cancel participation for event: " + eventDetails.getTitle() + " (To be implemented)");
        myStatusText.setText("Cancel feature to be implemented."); // Placeholder
    }

    @FXML
    public void handleBackToList() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/fxml/VolunteerScreen/VolunteerViewMyEventsScreen.fxml"));
            Parent root = loader.load();

            VolunteerViewMyEventsScreenHandler listController = loader.getController();
            listController.setStage(this.stage);
            listController.setVolunteer(this.volunteer); // Truyền lại volunteer

            Scene scene = new Scene(root);
            this.stage.setScene(scene);
            this.stage.setTitle("My Events List");
        } catch (IOException e) {
            e.printStackTrace();
            // Hiển thị lỗi cho người dùng nếu cần
        }
    }
}