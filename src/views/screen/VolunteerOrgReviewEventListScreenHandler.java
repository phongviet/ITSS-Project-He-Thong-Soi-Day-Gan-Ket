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
import javafx.scene.control.Button;
import javafx.scene.control.Label;
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
        // loadEvents() sẽ được gọi trong setOrganization(...)
    }

    private void setupTableColumns() {
        // Cột Title
        titleColumn.setCellValueFactory(cellData ->
            new SimpleStringProperty(cellData.getValue().getTitle()));

        // Cột Status
        statusColumn.setCellValueFactory(cellData ->
            new SimpleStringProperty(cellData.getValue().getStatus()));

        // Cột Action: nút "Edit" hoặc label "No Edit"
        actionColumn.setCellFactory(new Callback<TableColumn<Event, Void>, TableCell<Event, Void>>() {
            @Override
            public TableCell<Event, Void> call(TableColumn<Event, Void> param) {
                return new TableCell<Event, Void>() {
                    private final Button editButton = new Button("Edit");

                    {
                        editButton.setStyle("-fx-background-color: #f1c40f; -fx-text-fill: white;");
                        editButton.setOnAction(evt -> {
                            Event ev = getTableView().getItems().get(getIndex());
                            openEditParticipants(ev);
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
                                editButton.setDisable(false);
                                setGraphic(editButton);
                            } else {
                                Label canceledLabel = new Label("No Edit");
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

    private void openEditParticipants(Event event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(
                "/views/fxml/OrganizationScreen/VolunteerOrgEditParticipantsScreen.fxml"));
            Parent root = loader.load();

            VolunteerOrgEditParticipantsScreenHandler controller = loader.getController();
            controller.setStage(stage);
            controller.setOrganization(organization);
            controller.setEvent(event);

            Scene scene = new Scene(root);
            stage.setScene(scene);
            stage.setTitle("Edit Participant Details");
            stage.show();

        } catch (IOException e) {
            statusMessage.setText("Error loading edit participants screen: " + e.getMessage());
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
