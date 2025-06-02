package views.screen;

import controller.event.EventController;
import entity.events.Event;
import entity.events.EventParticipantDetails;
import entity.users.VolunteerOrganization;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import javafx.util.converter.DoubleStringConverter;

import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

public class VolunteerOrgReviewParticipantsScreenHandler implements Initializable {

    @FXML private Label eventTitleLabel;
    @FXML private TableView<EventParticipantDetailsRow> participantTable;
    @FXML private TableColumn<EventParticipantDetailsRow, String> volunteerColumn;
    @FXML private TableColumn<EventParticipantDetailsRow, Integer> hoursColumn;
    @FXML private TableColumn<EventParticipantDetailsRow, Integer> ratingByOrgColumn;
    @FXML private TableColumn<EventParticipantDetailsRow, Double> newScoreColumn;
    @FXML private Label statusMessage;

    private Stage stage;
    private VolunteerOrganization organization;
    private Event event;
    private EventController eventController;
    private ObservableList<EventParticipantDetailsRow> participantData;

    public VolunteerOrgReviewParticipantsScreenHandler(Stage stage, VolunteerOrganization org) {
        this.stage = stage;
        this.organization = org;
        this.eventController = new EventController();
    }

    public VolunteerOrgReviewParticipantsScreenHandler() {
        this.eventController = new EventController();
    }

    public void setStage(Stage s) {
        this.stage = s;
    }

    public void setOrganization(VolunteerOrganization org) {
        this.organization = org;
    }

    public void setEvent(Event event) {
        this.event = event;
        eventTitleLabel.setText(event.getTitle());
        loadParticipants();
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        setupTableColumns();
    }

    private void setupTableColumns() {
        // Cột volunteerUsername
        volunteerColumn.setCellValueFactory(cellData ->
            new SimpleStringProperty(cellData.getValue().getVolunteerUsername()));

        // Cột hoursParticipated (kiểu int primitive, không so sánh null)
        hoursColumn.setCellValueFactory(cellData ->
            new SimpleIntegerProperty(cellData.getValue().getHoursParticipated()).asObject());

        // Cột ratingByOrg (kiểu int primitive)
        ratingByOrgColumn.setCellValueFactory(cellData ->
            new SimpleIntegerProperty(cellData.getValue().getRatingByOrg()).asObject());

        // Cột newScore (Double), cho phép edit
        newScoreColumn.setCellValueFactory(cellData ->
            new SimpleDoubleProperty(cellData.getValue().getNewScore()).asObject());
        newScoreColumn.setCellFactory(TextFieldTableCell.<EventParticipantDetailsRow, Double>forTableColumn(
            new DoubleStringConverter()
        ));
        newScoreColumn.setEditable(true);

        newScoreColumn.setOnEditCommit(event -> {
            EventParticipantDetailsRow row = event.getRowValue();
            double input = event.getNewValue();
            if (input < 1 || input > 5) {
                statusMessage.setText("Rating chỉ được nhập từ 1 đến 5.");
                row.setNewScore(event.getOldValue());
                participantTable.refresh();
            } else {
                row.setNewScore(input);
                statusMessage.setText("");
            }
        });

        participantTable.setEditable(true);
    }

    private void loadParticipants() {
        if (event == null) {
            statusMessage.setText("Event chưa set.");
            return;
        }
        List<EventParticipantDetails> list = eventController.getEventParticipantDetails(event.getEventId());
        participantData = FXCollections.observableArrayList();
        for (EventParticipantDetails dto : list) {
            String volUser = dto.getVolunteerUsername();
            // dto.getHoursParticipated() trả về Integer, nếu null thì dùng 0
            int hours = dto.getHoursParticipated() != null ? dto.getHoursParticipated() : 0;
            int byOrg = dto.getRatingByOrg() != null ? dto.getRatingByOrg() : 0;
            participantData.add(new EventParticipantDetailsRow(volUser, hours, byOrg, 0.0));
        }
        participantTable.setItems(participantData);
    }

    @FXML
    public void handleSubmitRatings() {
        boolean allValid = true;
        for (EventParticipantDetailsRow row : participantData) {
            double score = row.getNewScore();
            if (score < 1 || score > 5) {
                allValid = false;
                break;
            }
        }
        if (!allValid) {
            statusMessage.setText("Vui lòng nhập điểm hợp lệ (1-5) cho tất cả volunteer.");
            return;
        }

        boolean anyFail = false;
        for (EventParticipantDetailsRow row : participantData) {
            int score = (int) row.getNewScore();
            boolean ok = eventController.updateVolunteerRating(row.getVolunteerUsername(), score);
            if (!ok) anyFail = true;
        }

        if (anyFail) {
            statusMessage.setText("Có lỗi xảy ra khi cập nhật một số volunteer.");
        } else {
            statusMessage.setText("Đã gửi đánh giá thành công!");
        }
    }

    @FXML
    public void handleBack() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(
                "/views/fxml/OrganizationScreen/VolunteerOrgReviewEventListScreen.fxml"));
            Parent root = loader.load();

            VolunteerOrgReviewEventListScreenHandler controller = loader.getController();
            controller.setStage(stage);
            controller.setOrganization(organization);

            Scene scene = new Scene(root);
            stage.setScene(scene);
            stage.setTitle("Review Events");
            stage.show();
        } catch (IOException e) {
            statusMessage.setText("Error returning to events list: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /** Lớp con để chứa dữ liệu mỗi hàng trong TableView */
    public static class EventParticipantDetailsRow {
        private final String volunteerUsername;
        private final int hoursParticipated;
        private final int ratingByOrg;
        private double newScore;

        public EventParticipantDetailsRow(String volunteerUsername, int hoursParticipated, int ratingByOrg, double newScore) {
            this.volunteerUsername = volunteerUsername;
            this.hoursParticipated = hoursParticipated;
            this.ratingByOrg = ratingByOrg;
            this.newScore = newScore;
        }

        public String getVolunteerUsername() {
            return volunteerUsername;
        }

        public int getHoursParticipated() {
            return hoursParticipated;
        }

        public int getRatingByOrg() {
            return ratingByOrg;
        }

        public double getNewScore() {
            return newScore;
        }

        public void setNewScore(double newScore) {
            this.newScore = newScore;
        }
    }
}
