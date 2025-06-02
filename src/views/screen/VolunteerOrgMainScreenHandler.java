package views.screen;

import controller.event.EventController;
import entity.users.VolunteerOrganization;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;
import entity.requests.HelpRequest;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.Button;
import java.util.Date;
import java.text.SimpleDateFormat;
import java.util.List;

public class VolunteerOrgMainScreenHandler implements Initializable {

    @FXML
    private Button registerEventButton;

    @FXML
    private Button viewEventsButton;

    @FXML
    private Label statusMessage;
    @FXML
    private TableView<HelpRequest> helpRequestTable;

    @FXML
    private TableColumn<HelpRequest, String> titleColumn;

    @FXML
    private TableColumn<HelpRequest, String> startDateColumn;

    @FXML
    private TableColumn<HelpRequest, String> emergencyLevelColumn;

    @FXML
    private TableColumn<HelpRequest, Void> actionColumn;

    // Formatter để hiển thị date
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");
    private Stage stage;
    private VolunteerOrganization organization;
    private EventController eventController;

    public VolunteerOrgMainScreenHandler(Stage stage, VolunteerOrganization organization) {
        this.stage = stage;
        this.organization = organization;
        this.eventController = new EventController();
    }

    // Add a default constructor
    public VolunteerOrgMainScreenHandler() {
        this.eventController = new EventController();
    }

    // Add setter methods
    public void setStage(Stage stage) {
        this.stage = stage;
    }

    public void setOrganization(VolunteerOrganization organization) {
        this.organization = organization;
    }

    public void setStatusMessage(String message) {
        statusMessage.setText(message);
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        // Clear any previous status message
        statusMessage.setText("");
        setupHelpRequestTable();
        loadHelpRequests();
    }
    /**
     * Thiết lập cách hiển thị các cột trong TableView<HelpRequest>
     */
    private void setupHelpRequestTable() {
        // Column "Title"
        titleColumn.setCellValueFactory(cellData ->
            new SimpleStringProperty(cellData.getValue().getTitle()));

        // Column "Start Date"
        startDateColumn.setCellValueFactory(cellData -> {
            Date d = cellData.getValue().getStartDate();
            String s = (d != null) ? DATE_FORMAT.format(d) : "N/A";
            return new SimpleStringProperty(s);
        });

        // Column "Emergency Level"
        emergencyLevelColumn.setCellValueFactory(cellData ->
            new SimpleStringProperty(cellData.getValue().getEmergencyLevel()));

        // Column "Nhận yêu cầu" (Action)
        actionColumn.setCellFactory(col -> new TableCell<HelpRequest, Void>() {
            private final Button acceptButton = new Button("Nhận yêu cầu");

            {
                acceptButton.setStyle("-fx-background-color: #2ecc71; -fx-text-fill: white;");
                acceptButton.setOnAction(event -> {
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
        });
    }

    /**
     * Lấy danh sách HelpRequest "approved" từ DB và đổ vào TableView
     */
    private void loadHelpRequests() {
        List<HelpRequest> helpRequests = eventController.getApprovedHelpRequests();
        if (helpRequests == null || helpRequests.isEmpty()) {
            statusMessage.setText("Không có yêu cầu hỗ trợ nào đang ở trạng thái 'approved'.");
        } else {
            ObservableList<HelpRequest> data = FXCollections.observableArrayList(helpRequests);
            helpRequestTable.setItems(data);
        }
    }
    private void handleAcceptHelpRequest(HelpRequest helpRequest) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(
                "/views/fxml/OrganizationScreen/VolunteerOrgRegisterEventScreen.fxml"));
            Parent root = loader.load();

            // Lấy controller của màn hình RegisterEvent
            VolunteerOrgRegisterEventScreenHandler controller = loader.getController();
            controller.setStage(stage);
            controller.setOrganization(organization);
            controller.setHelpRequest(helpRequest);    // truyền helpRequest để sau khi tạo xong sẽ xử lý

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
    public void handleRegisterEvent() {
        try {
            // Load the event registration screen
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/fxml/OrganizationScreen/VolunteerOrgRegisterEventScreen.fxml"));
            Parent root = loader.load();

            // Get the controller and pass the organization data
            VolunteerOrgRegisterEventScreenHandler controller = loader.getController();
            controller.setStage(stage);
            controller.setOrganization(organization);

            // Set the scene
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
    public void handleViewEvents() {
        try {
            // Load the event list screen
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/fxml/OrganizationScreen/VolunteerOrgViewEventListScreen.fxml"));
            Parent root = loader.load();

            // Get the controller and pass the organization data
            VolunteerOrgViewEventListScreenHandler controller = loader.getController();
            controller.setStage(stage);
            controller.setOrganization(organization);

            // Set the scene
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
    public void handleLogout() {
        try {
            // Load the login FXML
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/fxml/LogInScreen.fxml"));
            Parent root = loader.load();

            // Set the scene
            Scene scene = new Scene(root);
            stage.setScene(scene);
            stage.setTitle("Login");
            stage.show();
        } catch (IOException e) {
            statusMessage.setText("Error logging out: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
