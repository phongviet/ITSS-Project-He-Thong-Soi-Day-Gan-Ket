package views.screen;

import controller.requests.HelpRequestController;
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
    @FXML private TableColumn<HelpRequest, Void> actionColumn;
    @FXML private Label statusMessage;

    private VolunteerOrganization organization;
    private Stage stage;
    private HelpRequestController helpRequestController;
    private ObservableList<HelpRequest> helpRequestData;
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");

    public VolunteerOrgHelpRequestListScreenHandler(Stage stage, VolunteerOrganization org) {
        this.stage = stage;
        this.organization = org;
        this.helpRequestController = new HelpRequestController();
    }

    public VolunteerOrgHelpRequestListScreenHandler() {
        this.helpRequestController = new HelpRequestController();
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

        // cột hành động: nút "Nhận yêu cầu"
        actionColumn.setCellFactory(new Callback<TableColumn<HelpRequest, Void>, TableCell<HelpRequest, Void>>() {
            @Override
            public TableCell<HelpRequest, Void> call(TableColumn<HelpRequest, Void> param) {
                return new TableCell<HelpRequest, Void>() {
                    private final Button acceptButton = new Button("Accept");

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
        if (organization == null) {
            statusMessage.setText("Organization not set yet.");
            return;
        }

        List<HelpRequest> list = helpRequestController.getApprovedHelpRequests();
        if (list == null || list.isEmpty()) {
            helpRequestData = FXCollections.observableArrayList();
            statusMessage.setText("There are no HelpRequests in 'Approved' status.");
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
