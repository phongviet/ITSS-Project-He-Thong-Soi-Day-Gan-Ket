package views.screen;

import controller.event.EventController;
import entity.events.Event;
import entity.users.VolunteerOrganization;
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
import javafx.scene.control.TableView;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import javafx.util.Callback;

import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.ResourceBundle;

public class VolunteerOrgReviewEventListScreenHandler implements Initializable {

    @FXML private TableView<Event> eventTableView;
    @FXML private TableColumn<Event, String> titleColumn;
    @FXML private TableColumn<Event, String> statusColumn;
    @FXML private TableColumn<Event, Void> actionColumn;
    @FXML private Label statusMessage;

    private Stage stage;
    private VolunteerOrganization organization;
    private EventController eventController;
    private ObservableList<Event> eventData;

    public VolunteerOrgReviewEventListScreenHandler(Stage stage, VolunteerOrganization org) {
        this.stage = stage;
        this.organization = org;
        this.eventController = new EventController();
    }

    public VolunteerOrgReviewEventListScreenHandler() {
        this.eventController = new EventController();
    }

    public void setStage(Stage s) {
        this.stage = s;
    }

    public void setOrganization(VolunteerOrganization org) {
        this.organization = org;
        loadEvents();
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        setupTableColumns();
        // loadEvents() được gọi khi setOrganization
    }

    private void setupTableColumns() {
        // Title
        titleColumn.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getTitle()));

        // Status
        statusColumn.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getStatus()));

        // Hành động: nếu done thì nút "Review", nếu canceled thì label "Canceled" disabled
        actionColumn.setCellFactory(new Callback<TableColumn<Event, Void>, TableCell<Event, Void>>() {
            @Override
            public TableCell<Event, Void> call(TableColumn<Event, Void> param) {
                return new TableCell<Event, Void>() {
                    private final Button reviewButton = new Button("Review");

                    {
                        reviewButton.setStyle("-fx-background-color: #2ecc71; -fx-text-fill: white;");
                        reviewButton.setOnAction(evt -> {
                            Event ev = getTableView().getItems().get(getIndex());
                            openReviewParticipants(ev);
                        });
                    }

                    @Override
                    protected void updateItem(Void item, boolean empty) {
                        super.updateItem(item, empty);
                        if (empty) {
                            setGraphic(null);
                        } else {
                            Event ev = getTableView().getItems().get(getIndex());
                            if ("done".equalsIgnoreCase(ev.getStatus())) {
                                reviewButton.setDisable(false);
                                setGraphic(reviewButton);
                            } else {
                                // Nếu canceled, hiển thị một label "Canceled"
                                Label canceledLabel = new Label("Canceled");
                                canceledLabel.setStyle("-fx-text-fill: gray;");
                                setGraphic(canceledLabel);
                            }
                        }
                    }
                };
            }
        });
    }

    private void loadEvents() {
        if (organization == null) {
            statusMessage.setText("Organization chưa set.");
            return;
        }
        List<String> statuses = Arrays.asList("Done", "Canceled");
        List<Event> list = eventController.getEventsByStatusForOrganizer(organization.getUsername(), statuses);
        if (list == null || list.isEmpty()) {
            eventData = FXCollections.observableArrayList();
            statusMessage.setText("Không có event nào ở trạng thái done hoặc canceled.");
        } else {
            eventData = FXCollections.observableArrayList(list);
            statusMessage.setText("");
        }
        eventTableView.setItems(eventData);
    }

    private void openReviewParticipants(Event event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(
                "/views/fxml/OrganizationScreen/VolunteerOrgReviewParticipantsScreen.fxml"));
            Parent root = loader.load();

            VolunteerOrgReviewParticipantsScreenHandler controller = loader.getController();
            controller.setStage(stage);
            controller.setOrganization(organization);
            controller.setEvent(event);

            Scene scene = new Scene(root);
            stage.setScene(scene);
            stage.setTitle("Review Participants");
            stage.show();

        } catch (IOException e) {
            statusMessage.setText("Error loading participants screen: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    public void handleBackToDashboard() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(
                "/views/fxml/OrganizationScreen/VolunteerOrgMainScreen.fxml"));
            Parent root = loader.load();

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
