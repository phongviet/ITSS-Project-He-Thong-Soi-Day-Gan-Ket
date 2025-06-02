package views.screen;

import controller.event.EventController;
import entity.events.Event;
import entity.events.EventParticipantDetails;
import entity.users.VolunteerOrganization;
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
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import javafx.util.converter.IntegerStringConverter;

import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

public class VolunteerOrgEditParticipantsScreenHandler implements Initializable {

    @FXML private Label eventTitleLabel;
    @FXML private TableView<EventParticipantRow> participantTable;
    @FXML private TableColumn<EventParticipantRow, String> volunteerColumn;
    @FXML private TableColumn<EventParticipantRow, Integer> hoursColumn;
    @FXML private TableColumn<EventParticipantRow, Integer> ratingColumn;
    @FXML private TableColumn<EventParticipantRow, Void> actionColumn;
    @FXML private Label statusMessage;

    private Stage stage;
    private VolunteerOrganization organization;
    private Event event;
    private EventController eventController;
    private ObservableList<EventParticipantRow> participantData;

    public VolunteerOrgEditParticipantsScreenHandler(Stage stage, VolunteerOrganization org) {
        this.stage = stage;
        this.organization = org;
        this.eventController = new EventController();
    }

    public VolunteerOrgEditParticipantsScreenHandler() {
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
        loadParticipantDetails();
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        setupTableColumns();
    }

    private void setupTableColumns() {
        // Volunteer username
        volunteerColumn.setCellValueFactory(cellData ->
            new SimpleStringProperty(cellData.getValue().getVolunteerUsername()));

        // HoursParticipated (Integer), cho edit
        hoursColumn.setCellValueFactory(cellData ->
            new SimpleIntegerProperty(cellData.getValue().getHoursParticipated() == null ? 0 : cellData.getValue().getHoursParticipated()).asObject());
        hoursColumn.setCellFactory(TextFieldTableCell.<EventParticipantRow, Integer>forTableColumn(new IntegerStringConverter()));
        hoursColumn.setOnEditCommit(evt -> {
            EventParticipantRow row = evt.getRowValue();
            Integer newVal;
            try {
                newVal = evt.getNewValue();
            } catch (Exception ex) {
                // Nếu không parse được, để lại giá trị cũ
                newVal = row.getHoursParticipated();
            }
            row.setHoursParticipated(newVal);
        });

        // RatingByOrg (Integer), cho edit
        ratingColumn.setCellValueFactory(cellData ->
            new SimpleIntegerProperty(cellData.getValue().getRatingByOrg() == null ? 0 : cellData.getValue().getRatingByOrg()).asObject());
        ratingColumn.setCellFactory(TextFieldTableCell.<EventParticipantRow, Integer>forTableColumn(new IntegerStringConverter()));
        ratingColumn.setOnEditCommit(evt -> {
            EventParticipantRow row = evt.getRowValue();
            Integer newVal;
            try {
                newVal = evt.getNewValue();
            } catch (Exception ex) {
                newVal = row.getRatingByOrg();
            }
            row.setRatingByOrg(newVal);
        });

        // Cột hành động: nút Save
        actionColumn.setCellFactory(col -> new TableCell<EventParticipantRow, Void>() {
            private final Button btn = new Button("Save");

            {
                btn.setStyle("-fx-background-color: #3498db; -fx-text-fill: white;");
                btn.setOnAction(evt -> {
                    EventParticipantRow row = getTableView().getItems().get(getIndex());
                    saveOneRow(row);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    setGraphic(btn);
                }
            }
        });

        participantTable.setEditable(true);
    }

    private void loadParticipantDetails() {
        if (event == null) {
            statusMessage.setText("Event chưa set.");
            return;
        }
        // Lấy danh sách EventParticipantDetails từ EventController
        List<EventParticipantDetails> list = eventController.getParticipantDetailsForEvent(event.getEventId());
        participantData = FXCollections.observableArrayList();
        for (EventParticipantDetails dto : list) {
            EventParticipantRow row = new EventParticipantRow(
                dto.getVolunteerUsername(),
                dto.getHoursParticipated(),
                dto.getRatingByOrg()
            );
            participantData.add(row);
        }
        participantTable.setItems(participantData);
    }

    private void saveOneRow(EventParticipantRow row) {
        boolean ok = eventController.updateEventParticipantDetails(
            event.getEventId(),
            row.getVolunteerUsername(),
            row.getHoursParticipated(),
            row.getRatingByOrg()
        );
        if (ok) {
            statusMessage.setText("Saved for " + row.getVolunteerUsername());
        } else {
            statusMessage.setText("Failed to save for " + row.getVolunteerUsername());
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

    /** 
     * Lớp con để làm row trong TableView 
     */
    public static class EventParticipantRow {
        private final String volunteerUsername;
        private Integer hoursParticipated;
        private Integer ratingByOrg;

        public EventParticipantRow(String volunteerUsername, Integer hoursParticipated, Integer ratingByOrg) {
            this.volunteerUsername = volunteerUsername;
            this.hoursParticipated = hoursParticipated;
            this.ratingByOrg = ratingByOrg;
        }

        public String getVolunteerUsername() {
            return volunteerUsername;
        }

        public Integer getHoursParticipated() {
            return hoursParticipated;
        }

        public void setHoursParticipated(Integer hoursParticipated) {
            this.hoursParticipated = hoursParticipated;
        }

        public Integer getRatingByOrg() {
            return ratingByOrg;
        }

        public void setRatingByOrg(Integer ratingByOrg) {
            this.ratingByOrg = ratingByOrg;
        }
    }
}
