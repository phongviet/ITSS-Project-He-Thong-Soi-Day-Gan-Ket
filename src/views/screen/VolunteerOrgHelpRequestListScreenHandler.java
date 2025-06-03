package views.screen;

import controller.event.EventController;
import entity.requests.HelpRequest;
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
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.ResourceBundle;

public class VolunteerOrgHelpRequestListScreenHandler implements Initializable {

    @FXML private TableView<HelpRequest> helpRequestTable;
    @FXML private TableColumn<HelpRequest, String> titleColumn;
    @FXML private TableColumn<HelpRequest, String> startDateColumn;
    @FXML private TableColumn<HelpRequest, String> emergencyLevelColumn;
    @FXML private TableColumn<HelpRequest, String> statusColumn;
    @FXML private TableColumn<HelpRequest, Void> actionColumn;
    @FXML private Label statusMessage;
    @FXML private Label screenTitleLabel;

    private VolunteerOrganization organization;
    private Stage stage;
    private EventController eventController;
    private ObservableList<HelpRequest> helpRequestData;
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");

    public VolunteerOrgHelpRequestListScreenHandler(Stage stage, VolunteerOrganization org) {
        this.stage = stage;
        this.organization = org;
        this.eventController = new EventController();
    }

    public VolunteerOrgHelpRequestListScreenHandler() {
        this.eventController = new EventController();
    }

    public void setStage(Stage s) {
        this.stage = s;
    }

    public void setOrganization(VolunteerOrganization org) {
        this.organization = org;
        // Khi có org, load lại dữ liệu
        loadHelpRequests();
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        setupTableColumns();
        // Chỉ thiết lập column, còn load data sau khi setOrganization
    }

    private void setupTableColumns() {
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

        statusColumn.setCellValueFactory(cellData ->
            new SimpleStringProperty(cellData.getValue().getStatus()));

        actionColumn.setCellFactory(new Callback<TableColumn<HelpRequest, Void>, TableCell<HelpRequest, Void>>() {
            @Override
            public TableCell<HelpRequest, Void> call(TableColumn<HelpRequest, Void> param) {
                return new TableCell<HelpRequest, Void>() {
                    private final Button acceptButton = new Button("Nhận yêu cầu");
                    private final Button satisfyButton = new Button("Xác nhận TG");
                    private final HBox pane = new HBox(5);

                    {
                        acceptButton.setStyle("-fx-background-color: #2ecc71; -fx-text-fill: white;");
                        acceptButton.setOnAction(evt -> {
                            HelpRequest hr = getTableView().getItems().get(getIndex());
                            handleAcceptHelpRequest(hr);
                        });

                        satisfyButton.setStyle("-fx-background-color: #3498db; -fx-text-fill: white;");
                        satisfyButton.setOnAction(evt -> {
                            HelpRequest hr = getTableView().getItems().get(getIndex());
                            handleSatisfyHelpRequest(hr);
                        });
                    }

                    @Override
                    protected void updateItem(Void item, boolean empty) {
                        super.updateItem(item, empty);
                        if (empty) {
                            setGraphic(null);
                        } else {
                            HelpRequest hr = getTableView().getItems().get(getIndex());
                            pane.getChildren().clear();
                            if ("Approved".equalsIgnoreCase(hr.getStatus())) {
                                pane.getChildren().add(acceptButton);
                            } else if ("Closed".equalsIgnoreCase(hr.getStatus())) {
                                pane.getChildren().add(satisfyButton);
                            }
                            setGraphic(pane);
                        }
                    }
                };
            }
        });
    }

    private void loadHelpRequests() {
        if (organization == null) {
            statusMessage.setText("Organization chưa được set.");
            return;
        }

        List<HelpRequest> list = eventController.getApprovedHelpRequests();
        if (list == null || list.isEmpty()) {
            helpRequestData = FXCollections.observableArrayList();
            statusMessage.setText("Không có yêu cầu trợ giúp nào được tìm thấy.");
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

    private void handleSatisfyHelpRequest(HelpRequest helpRequest) {
        System.out.println("Attempting to mark HelpRequest ID: " + helpRequest.getRequestId() + " as Satisfied.");
        boolean success = eventController.updateHelpRequestStatus(helpRequest.getRequestId(), "Satisfied");

        if (success) {
            statusMessage.setText("Yêu cầu ID: " + helpRequest.getRequestId() + " đã được xác nhận trợ giúp thành công.");
            loadHelpRequests();
        } else {
            statusMessage.setText("Lỗi khi xác nhận trợ giúp cho yêu cầu ID: " + helpRequest.getRequestId() + ".");
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
