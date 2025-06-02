package views.screen;

import controller.event.EventController;
import entity.requests.HelpRequest;
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
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Callback;

import java.io.IOException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.ResourceBundle;

public class VolunteerOrgMainScreenHandler implements Initializable {
    @FXML
    private Button reviewButton;
    @FXML
    private Button registerEventButton;

    @FXML
    private Button viewEventsButton;

    @FXML
    private Button listHelpRequestButton;

    @FXML
    private Button listRegistButton;

    @FXML
    private Label statusMessage;

    // ================= HelpRequest Pane =================
    @FXML
    private VBox helpRequestPane;

    @FXML
    private TableView<HelpRequest> helpRequestTable;

    @FXML
    private TableColumn<HelpRequest, String> titleColumn;

    @FXML
    private TableColumn<HelpRequest, String> startDateColumn;

    @FXML
    private TableColumn<HelpRequest, String> emergencyLevelColumn;

    @FXML
    private TableColumn<HelpRequest, Void> helpRequestActionColumn;

    private ObservableList<HelpRequest> helpRequestData;

    // Formatter để hiển thị ngày
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");

    // ================= Notification Pane =================
    @FXML
    private VBox notificationPane;

    @FXML
    private TableView<Notification> notificationTable;

    @FXML
    private TableColumn<Notification, String> eventTitleColumn;

    @FXML
    private TableColumn<Notification, String> volunteerColumn;

    @FXML
    private TableColumn<Notification, Void> notificationActionColumn;

    private ObservableList<Notification> notificationData;

    private Stage stage;
    private VolunteerOrganization organization;
    private EventController eventController;

    public VolunteerOrgMainScreenHandler(Stage stage, VolunteerOrganization organization) {
        this.stage = stage;
        this.organization = organization;
        this.eventController = new EventController();
    }

    public VolunteerOrgMainScreenHandler() {
        this.eventController = new EventController();
    }

    public void setStage(Stage stage) {
        this.stage = stage;
    }

    public void setOrganization(VolunteerOrganization organization) {
        this.organization = organization;
        // Chỉ load dữ liệu sau khi organization đã được gán
        loadHelpRequests();
        loadPendingNotifications();
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        statusMessage.setText("");

        // Chỉ thiết lập cấu hình cột, không load dữ liệu
        setupHelpRequestTable();
        setupNotificationTable();

        // Ẩn cả hai pane lúc khởi tạo
        helpRequestPane.setVisible(false);
        notificationPane.setVisible(false);
    }

    // ===================== HelpRequest =====================

    private void setupHelpRequestTable() {
        titleColumn.setCellValueFactory(cellData ->
            new SimpleStringProperty(cellData.getValue().getTitle()));

        startDateColumn.setCellValueFactory(cellData -> {
            if (cellData.getValue().getStartDate() != null) {
                return new SimpleStringProperty(DATE_FORMAT.format(cellData.getValue().getStartDate()));
            } else {
                return new SimpleStringProperty("N/A");
            }
        });

        emergencyLevelColumn.setCellValueFactory(cellData ->
            new SimpleStringProperty(cellData.getValue().getEmergencyLevel()));

        helpRequestActionColumn.setCellFactory(new Callback<TableColumn<HelpRequest, Void>, TableCell<HelpRequest, Void>>() {
            @Override
            public TableCell<HelpRequest, Void> call(TableColumn<HelpRequest, Void> param) {
                return new TableCell<HelpRequest, Void>() {
                    private final Button acceptButton = new Button("Nhận yêu cầu");

                    {
                        acceptButton.setStyle("-fx-background-color: #2ecc71; -fx-text-fill: white;");
                        acceptButton.setOnAction(evt -> {
                            HelpRequest hr = getTableView().getItems().get(getIndex());
                            handleAcceptHelpRequest(hr);
                        });
                    }

                    @Override
                    protected void updateItem(Void item, boolean empty) {
                        super.updateItem(item, empty);
                        if (empty) {
                            setGraphic(null);
                        } else {
                            setGraphic(acceptButton);
                        }
                    }
                };
            }
        });
    }

    private void loadHelpRequests() {
        if (organization == null) return; // tránh gọi khi organization chưa set

        List<HelpRequest> list = eventController.getApprovedHelpRequests();
        if (list == null || list.isEmpty()) {
            helpRequestData = FXCollections.observableArrayList();
        } else {
            helpRequestData = FXCollections.observableArrayList(list);
            statusMessage.setText("");
        }
        helpRequestTable.setItems(helpRequestData);
    }

    private void handleAcceptHelpRequest(HelpRequest helpRequest) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(
                    "/views/fxml/OrganizationScreen/VolunteerOrgRegisterEventScreen.fxml"));
            Parent root = loader.load();

            VolunteerOrgRegisterEventScreenHandler controller = loader.getController();
            controller.setStage(stage);
            controller.setOrganization(organization);
            controller.setHelpRequest(helpRequest);

            Scene scene = new Scene(root);
            stage.setScene(scene);
            stage.setTitle("Register New Event");
            stage.show();
        } catch (IOException e) {
            statusMessage.setText("Error loading registration screen: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // ===================== Notification =====================

    private void setupNotificationTable() {
        eventTitleColumn.setCellValueFactory(cellData ->
            new SimpleStringProperty(cellData.getValue().getEventTitle()));

        volunteerColumn.setCellValueFactory(cellData ->
            new SimpleStringProperty(cellData.getValue().getUsername()));

        notificationActionColumn.setCellFactory(new Callback<TableColumn<Notification, Void>, TableCell<Notification, Void>>() {
            @Override
            public TableCell<Notification, Void> call(TableColumn<Notification, Void> param) {
                return new TableCell<Notification, Void>() {
                    private final Button acceptButton = new Button("Đồng ý");
                    private final Button rejectButton = new Button("Từ chối");

                    {
                        acceptButton.setStyle("-fx-background-color: #2ecc71; -fx-text-fill: white;");
                        rejectButton.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white;");

                        acceptButton.setOnAction(evt -> {
                            Notification no = getTableView().getItems().get(getIndex());
                            handleNotificationDecision(no, "registered");
                        });
                        rejectButton.setOnAction(evt -> {
                            Notification no = getTableView().getItems().get(getIndex());
                            handleNotificationDecision(no, "canceled");
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
        if (organization == null) return; // tránh gọi khi organization chưa set

        List<Notification> list = eventController.getPendingNotificationsByOrganizer(organization.getUsername());
        if (list == null || list.isEmpty()) {
            notificationData = FXCollections.observableArrayList();
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
            statusMessage.setText("Updated notification " + no.getNotificationId() + " → " + newStatus);
        } else {
            statusMessage.setText("Failed to update notification " + no.getNotificationId());
        }
    }

    // ===================== Handlers cho các nút =====================

    @FXML
    public void handleRegisterEvent() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(
                    "/views/fxml/OrganizationScreen/VolunteerOrgRegisterEventScreen.fxml"));
            Parent root = loader.load();

            VolunteerOrgRegisterEventScreenHandler controller = loader.getController();
            controller.setStage(stage);
            controller.setOrganization(organization);

            Scene scene = new Scene(root);
            stage.setScene(scene);
            stage.setTitle("Register New Event");
            stage.show();
        } catch (IOException e) {
            statusMessage.setText("Error loading registration screen: " + e.getMessage());
            e.printStackTrace();
        }
    }
    @FXML
    public void handleReview() {
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
            statusMessage.setText("Error loading review screen: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    public void handleViewEvents() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(
                    "/views/fxml/OrganizationScreen/VolunteerOrgViewEventListScreen.fxml"));
            Parent root = loader.load();

            VolunteerOrgViewEventListScreenHandler controller = loader.getController();
            controller.setStage(stage);
            controller.setOrganization(organization);

            Scene scene = new Scene(root);
            stage.setScene(scene);
            stage.setTitle("My Events List");
            stage.show();
        } catch (IOException e) {
            statusMessage.setText("Error loading events list screen: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    public void handleListHelpRequest() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(
                "/views/fxml/OrganizationScreen/VolunteerOrgHelpRequestListScreen.fxml"));
            Parent root = loader.load();

            VolunteerOrgHelpRequestListScreenHandler controller = loader.getController();
            controller.setStage(stage);
            controller.setOrganization(organization);

            Scene scene = new Scene(root);
            stage.setScene(scene);
            stage.setTitle("List Help Requests");
            stage.show();
        } catch (IOException e) {
            statusMessage.setText("Error loading HelpRequest list screen: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    public void handleListRegist() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(
                "/views/fxml/OrganizationScreen/VolunteerOrgRegistListScreen.fxml"));
            Parent root = loader.load();

            VolunteerOrgRegistListScreenHandler controller = loader.getController();
            controller.setStage(stage);
            controller.setOrganization(organization);

            Scene scene = new Scene(root);
            stage.setScene(scene);
            stage.setTitle("List Regist");
            stage.show();
        } catch (IOException e) {
            statusMessage.setText("Error loading register list screen: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    public void handleLogout() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/fxml/LogInScreen.fxml"));
            Parent root = loader.load();

            Scene scene = new Scene(root, 1024, 768);
            stage.setScene(scene);
            stage.setTitle("Login");
            stage.show();
        } catch (IOException e) {
            statusMessage.setText("Error logging out: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
