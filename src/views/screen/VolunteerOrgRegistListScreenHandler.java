package views.screen;

import controller.event.EventController;
import entity.notifications.Notification;
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
import java.util.List;
import java.util.ResourceBundle;

public class VolunteerOrgRegistListScreenHandler implements Initializable {

    @FXML private TableView<Notification> notificationTable;
    @FXML private TableColumn<Notification, String> eventTitleColumn;
    @FXML private TableColumn<Notification, String> volunteerColumn;
    @FXML private TableColumn<Notification, Void> notificationActionColumn;
    @FXML private Label statusMessage;

    private VolunteerOrganization organization;
    private Stage stage;
    private EventController eventController;
    private ObservableList<Notification> notificationData;

    public VolunteerOrgRegistListScreenHandler(Stage stage, VolunteerOrganization org) {
        this.stage = stage;
        this.organization = org;
        this.eventController = new EventController();
    }

    public VolunteerOrgRegistListScreenHandler() {
        this.eventController = new EventController();
    }

    public void setStage(Stage s) {
        this.stage = s;
    }

    public void setOrganization(VolunteerOrganization org) {
        this.organization = org;
        // Khi có org, load dữ liệu
        loadPendingNotifications();
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        setupTableColumns();
        // Load data khi setOrganization được gọi
    }

    private void setupTableColumns() {
        eventTitleColumn.setCellValueFactory(cellData ->
            new SimpleStringProperty(cellData.getValue().getEventTitle()));

        volunteerColumn.setCellValueFactory(cellData ->
            new SimpleStringProperty(cellData.getValue().getUsername()));

        // cột hành động: hai nút "Đồng ý" và "Từ chối"
        notificationActionColumn.setCellFactory(new Callback<TableColumn<Notification, Void>, TableCell<Notification, Void>>() {
            @Override
            public TableCell<Notification, Void> call(TableColumn<Notification, Void> param) {
                return new TableCell<Notification, Void>() {
                    private final Button acceptButton = new Button("Accept");
                    private final Button rejectButton = new Button("Reject");

                    {
                        acceptButton.setStyle("-fx-background-color: #2ecc71; -fx-text-fill: white;");
                        rejectButton.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white;");

                        acceptButton.setOnAction(evt -> {
                            Notification no = getTableView().getItems().get(getIndex());
                            handleNotificationDecision(no, "Registered");
                        });
                        rejectButton.setOnAction(evt -> {
                            Notification no = getTableView().getItems().get(getIndex());
                            handleNotificationDecision(no, "Canceled");
                        });
                    }

                    @Override
                    protected void updateItem(Void item, boolean empty) {
                        super.updateItem(item, empty);
                        if (empty) {
                            setGraphic(null);
                        } else {
                            HBox box = new HBox(10, acceptButton, rejectButton);
                            setGraphic(box);
                        }
                    }
                };
            }
        });
    }

    private void loadPendingNotifications() {
        if (organization == null) {
            statusMessage.setText("Organization not set yet.");
            return;
        }

        List<Notification> list = eventController.getPendingNotificationsByOrganizer(organization.getUsername());
        if (list == null || list.isEmpty()) {
            notificationData = FXCollections.observableArrayList();
            statusMessage.setText("There are no pending registrations.");
        } else {
            notificationData = FXCollections.observableArrayList(list);
            statusMessage.setText("");
        }
        notificationTable.setItems(notificationData);
    }

    private void handleNotificationDecision(Notification no, String newStatus) {
        boolean ok = eventController.updateNotificationStatus(no.getNotificationId(), newStatus);
        if (ok) {
            notificationData.remove(no);
            statusMessage.setText("Status updated for registration" + no.getNotificationId() + " → " + newStatus);
        } else {
            statusMessage.setText("Unable to update status for registration " + no.getNotificationId());
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
