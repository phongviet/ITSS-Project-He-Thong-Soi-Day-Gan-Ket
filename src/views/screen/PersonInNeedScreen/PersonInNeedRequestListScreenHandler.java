package views.screen.PersonInNeedScreen;

import controller.requests.HelpRequestController;
import entity.requests.HelpRequest;
import entity.users.PersonInNeed; // Assuming you have this entity
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Optional;

public class PersonInNeedRequestListScreenHandler {

    @FXML private TableView<HelpRequest> requestsTableView;
    @FXML private TableColumn<HelpRequest, String> titleColumn;
    @FXML private TableColumn<HelpRequest, String> descriptionColumn;
    @FXML private TableColumn<HelpRequest, String> statusColumn;
    @FXML private TableColumn<HelpRequest, String> startDateColumn;
    @FXML private TableColumn<HelpRequest, String> emergencyColumn;
    @FXML private TableColumn<HelpRequest, Void> actionsColumn;
    @FXML private TableColumn<HelpRequest, Void> markSatisfiedColumn;
    @FXML private Label statusMessageLabel;
    @FXML private Button backToDashboardButton; // Added fx:id in FXML

    private Stage stage;
    private PersonInNeed currentUser;
    private final SimpleDateFormat tableDateFormat = new SimpleDateFormat("yyyy-MM-dd");
    private final HelpRequestController helpRequestController;

    public PersonInNeedRequestListScreenHandler() {
        this.helpRequestController = new HelpRequestController();
    }

    public void setStage(Stage stage) {
        this.stage = stage;
    }

    public void setCurrentUser(PersonInNeed user) {
        this.currentUser = user;
        if (this.currentUser == null) {
             if(backToDashboardButton != null) backToDashboardButton.setDisable(true);
             statusMessageLabel.setText("User not identified. Cannot load requests or go to dashboard.");
             statusMessageLabel.setStyle("-fx-text-fill: red;");
        } else {
            if(backToDashboardButton != null) backToDashboardButton.setDisable(false);
            loadUserRequests(); // Load requests when user is set
        }
    }

    @FXML
    public void initialize() {
        titleColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getTitle()));
        descriptionColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getDescription()));
        statusColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getStatus()));
        startDateColumn.setCellValueFactory(cellData -> {
            if (cellData.getValue().getStartDate() != null) {
                return new SimpleStringProperty(tableDateFormat.format(cellData.getValue().getStartDate()));
            }
            return new SimpleStringProperty("N/A");
        });
        emergencyColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getEmergencyLevel()));
        setupActionsColumn();
        setupMarkSatisfiedColumn();
        statusMessageLabel.setText("");
    }

    private void loadUserRequests() {
        if (currentUser == null || currentUser.getId() == 0) { // Assuming PersonInNeed has getId() and 0 is invalid
            statusMessageLabel.setText("User not properly identified. Cannot load requests.");
            statusMessageLabel.setStyle("-fx-text-fill: red;");
            requestsTableView.setItems(FXCollections.emptyObservableList());
            return;
        }
        List<HelpRequest> userRequests = helpRequestController.getHelpRequestsByUsername(currentUser.getUsername());
        ObservableList<HelpRequest> observableRequests = FXCollections.observableArrayList(userRequests);
        requestsTableView.setItems(observableRequests);
        if (userRequests.isEmpty()) {
            statusMessageLabel.setText("You have no help requests yet.");
        } else {
            statusMessageLabel.setText("");
        }
    }
    
    private void setupActionsColumn() {
        actionsColumn.setCellFactory(param -> new TableCell<HelpRequest, Void>() {
            private final Button editButton = new Button("Edit");
            private final Button deleteButton = new Button("Delete");
            private final HBox pane = new HBox(5, editButton, deleteButton);

            {
                pane.setAlignment(Pos.CENTER);
                editButton.setStyle("-fx-background-color: #f39c12; -fx-text-fill: white;");
                editButton.setOnAction(event -> {
                    HelpRequest request = getTableView().getItems().get(getIndex());
                    handleEditRequest(request);
                });
                deleteButton.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white;");
                deleteButton.setOnAction(event -> {
                    HelpRequest request = getTableView().getItems().get(getIndex());
                    handleDeleteRequest(request);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getIndex() < 0 || getIndex() >= getTableView().getItems().size()) {
                    setGraphic(null);
                } else {
                    HelpRequest request = getTableView().getItems().get(getIndex());
                    // Only allow edit/delete if status is "Pending" (or similar logic)
                    if (request != null && "Pending".equalsIgnoreCase(request.getStatus())) {
                        setGraphic(pane);
                    } else {
                        setGraphic(null); 
                    }
                }
            }
        });
    }

    private void setupMarkSatisfiedColumn() {
        markSatisfiedColumn.setCellFactory(param -> new TableCell<HelpRequest, Void>() {
            private final Button markSatisfiedButton = new Button("Mark Satisfied");
            private final HBox pane = new HBox(markSatisfiedButton);

            {
                pane.setAlignment(Pos.CENTER);
                markSatisfiedButton.setStyle("-fx-background-color: #28a745; -fx-text-fill: white;"); // Green button
                markSatisfiedButton.setOnAction(event -> {
                    HelpRequest request = getTableView().getItems().get(getIndex());
                    if (request != null) {
                        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
                        alert.setTitle("Confirmm");
                        alert.setHeaderText("Mark the request as \"Satisfied\"?");
                        alert.setContentText("Are you sure you want to mark the request \"" + request.getTitle() + "\" as \"Satisfied\"?");
                        
                        Optional<ButtonType> result = alert.showAndWait();
                        if (result.isPresent() && result.get() == ButtonType.OK) {
                            boolean success = helpRequestController.markAsSatisfied(request.getRequestId());
                            if (success) {
                                statusMessageLabel.setText("The request \"" + request.getTitle() + "\" has been marked as Satisfied.");
                                statusMessageLabel.setStyle("-fx-text-fill: green;");
                                loadUserRequests(); // Refresh the list
                            } else {
                                statusMessageLabel.setText("Error: Unable to update status for request \"" + request.getTitle() + "\".");
                                statusMessageLabel.setStyle("-fx-text-fill: red;");
                            }
                        }
                    }
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                // Check getIndex bounds to prevent IndexOutOfBoundsException
                if (empty || getIndex() < 0 || getIndex() >= getTableView().getItems().size()) {
                    setGraphic(null);
                } else {
                    HelpRequest request = getTableView().getItems().get(getIndex());
                    if (request != null && "Closed".equalsIgnoreCase(request.getStatus())) {
                        setGraphic(pane);
                    } else {
                        setGraphic(null);
                    }
                }
            }
        });
    }

    private void handleEditRequest(HelpRequest request) {
        if (currentUser == null) return;
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/fxml/PersonInNeedScreen/PersonInNeedCreateRequestScreen.fxml"));
            Parent root = loader.load();
            PersonInNeedCreateRequestScreenHandler controller = loader.getController();
            controller.setStage(stage);
            controller.setCurrentUser(currentUser);
            controller.populateForEdit(request);

            Scene scene = new Scene(root);
            stage.setScene(scene);
            stage.setTitle("Edit Help Request: " + request.getTitle());
        } catch (IOException e) {
            e.printStackTrace();
            statusMessageLabel.setText("Error loading edit screen: " + e.getMessage());
            statusMessageLabel.setStyle("-fx-text-fill: red;");
        }
    }

    private void handleDeleteRequest(HelpRequest request) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirm Deletion");
        alert.setHeaderText("Delete Help Request: '" + request.getTitle() + "'?");
        alert.setContentText("Are you sure you want to delete this request? This action cannot be undone.");

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            boolean success = helpRequestController.deleteHelpRequest(request.getRequestId());
            if (success) {
                statusMessageLabel.setText("Request '" + request.getTitle() + "' deleted successfully.");
                statusMessageLabel.setStyle("-fx-text-fill: green;");
                loadUserRequests(); // Refresh list
            } else {
                statusMessageLabel.setText("Failed to delete request '" + request.getTitle() + "'.");
                statusMessageLabel.setStyle("-fx-text-fill: red;");
            }
        }
    }

    @FXML
    private void handleCreateNewRequest() {
        if (currentUser == null) {
            statusMessageLabel.setText("Cannot create new request: User not identified.");
            statusMessageLabel.setStyle("-fx-text-fill: red;");
            return;
        }
         try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/fxml/PersonInNeedScreen/PersonInNeedCreateRequestScreen.fxml"));
            Parent root = loader.load();
            PersonInNeedCreateRequestScreenHandler controller = loader.getController();
            controller.setStage(stage);
            controller.setCurrentUser(currentUser);
            Scene scene = new Scene(root);
            stage.setScene(scene);
            stage.setTitle("Create New Help Request");
        } catch (IOException e) {
            e.printStackTrace();
            statusMessageLabel.setText("Error loading create request screen: " + e.getMessage());
            statusMessageLabel.setStyle("-fx-text-fill: red;");
        }
    }
    
    @FXML
    private void handleBackToDashboard() {
        if (currentUser == null || stage == null) { 
            statusMessageLabel.setText("Cannot go to dashboard: User or Stage not identified.");
            statusMessageLabel.setStyle("-fx-text-fill: red;");
            return;
        }
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/fxml/PersonInNeedScreen/PersonInNeedMainScreen.fxml")); 
            Parent newRoot = loader.load();
            
            views.screen.PersonInNeedMainScreenHandler controller = loader.getController();
            controller.setStage(stage); 
            controller.setPersonInNeed(currentUser); 
            
            Scene currentScene = stage.getScene();
            if (currentScene != null) {
                currentScene.setRoot(newRoot);
            } else {
                stage.setScene(new Scene(newRoot)); 
            }
            stage.setTitle(currentUser.getName() + "'s Dashboard");
        } catch (IOException e) {
            e.printStackTrace();
             statusMessageLabel.setText("Error loading dashboard screen: " + e.getMessage());
             statusMessageLabel.setStyle("-fx-text-fill: red;");
        }
    }
} 