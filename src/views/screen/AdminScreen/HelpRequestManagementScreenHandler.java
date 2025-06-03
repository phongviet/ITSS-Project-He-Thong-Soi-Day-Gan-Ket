package views.screen.AdminScreen;

import controller.AdminApprovalController;
import entity.requests.HelpRequest;
import entity.users.Admin;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import javafx.util.Callback;

import java.io.IOException;
import java.net.URL;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Calendar;
import java.util.List;
import java.util.ResourceBundle;
import java.util.function.Predicate;

public class HelpRequestManagementScreenHandler implements Initializable {

    @FXML
    private TableView<HelpRequest> helpRequestTableView;

    @FXML
    private TableColumn<HelpRequest, String> titleColumn;

    @FXML
    private TableColumn<HelpRequest, String> requesterColumn;

    @FXML
    private TableColumn<HelpRequest, String> startDateColumn;

    @FXML
    private TableColumn<HelpRequest, String> emergencyLevelColumn;

    @FXML
    private TableColumn<HelpRequest, String> statusColumn;

    @FXML
    private TableColumn<HelpRequest, Void> actionsColumn;

    @FXML
    private TextField searchField;

    @FXML
    private ComboBox<String> statusFilterComboBox;

    @FXML
    private Label statusMessage;

    @FXML
    private Button backButton;

    private Stage stage;
    private Admin admin;
    private AdminApprovalController adminApprovalController;

    // Store the original unfiltered data
    private ObservableList<HelpRequest> allRequestsList = FXCollections.observableArrayList();

    // Store the filtered data
    private FilteredList<HelpRequest> filteredRequestList;

    // Current filters
    private String currentSearchText = "";
    private String currentStatusFilter = "All";

    public HelpRequestManagementScreenHandler(Stage stage, Admin admin) {
        this.stage = stage;
        this.admin = admin;
        initializeController();
    }

    // Default constructor needed for FXML loader
    public HelpRequestManagementScreenHandler() {
    }

    // Setter methods
    public void setStage(Stage stage) {
        this.stage = stage;
    }

    public void setAdmin(Admin admin) {
        this.admin = admin;
        // Initialize the controller when admin is set
        initializeController();

        // After setting admin and initializing controller, load the help requests data
        if (helpRequestTableView != null) { // Check if the FXML elements are already initialized
            loadHelpRequestsData();
        }
    }

    /**
     * Initialize the controller with the admin user
     */
    private void initializeController() {
        if (admin != null) {
            this.adminApprovalController = new AdminApprovalController();
        }
    }

    public void setStatusMessage(String message) {
        statusMessage.setText(message);
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        // Clear any previous status message
        statusMessage.setText("");

        // Setup table columns
        setupTableColumns();

        // Setup filters
        setupFilters();

        // Load help requests data if controller is initialized
        if (adminApprovalController != null) {
            loadHelpRequestsData();
        }
    }

    private void setupTableColumns() {
        // Setup title column
        titleColumn.setCellValueFactory(cellData ->
            new SimpleStringProperty(cellData.getValue().getTitle()));

        // Setup requester column
        requesterColumn.setCellValueFactory(cellData ->
            new SimpleStringProperty(cellData.getValue().getPersonInNeedID()));

        // Setup start date column
        startDateColumn.setCellValueFactory(cellData -> {
            HelpRequest request = cellData.getValue();
            if (request.getStartDate() != null) {
                Calendar cal = Calendar.getInstance();
                cal.setTime(request.getStartDate());
                String startDate = String.format("%02d/%02d/%d",
                    cal.get(Calendar.DAY_OF_MONTH),
                    cal.get(Calendar.MONTH) + 1,
                    cal.get(Calendar.YEAR));
                return new SimpleStringProperty(startDate);
            } else {
                return new SimpleStringProperty("N/A");
            }
        });

        // Setup emergency level column
        emergencyLevelColumn.setCellValueFactory(cellData ->
            new SimpleStringProperty(cellData.getValue().getEmergencyLevel()));

        // Setup status column
        statusColumn.setCellValueFactory(cellData -> {
            String status = cellData.getValue().getStatus();
            if (status == null || status.isEmpty()) {
                status = "Pending";
            } else {
                // Capitalize first letter for display purposes
                status = status.substring(0, 1).toUpperCase() + status.substring(1).toLowerCase();
            }
            return new SimpleStringProperty(status);
        });

        // Setup action column
        setupActionsColumn();
    }

    private void setupFilters() {
        // Initialize the status filter combo box
        statusFilterComboBox.setValue("All");

        // Add listeners to the search field and status filter combo box
        searchField.textProperty().addListener((observable, oldValue, newValue) -> {
            currentSearchText = newValue;
            applyFilters();
        });

        statusFilterComboBox.valueProperty().addListener((observable, oldValue, newValue) -> {
            currentStatusFilter = newValue;
            applyFilters();
        });
    }

    /**
     * Apply both search and status filters
     */
    private void applyFilters() {
        Predicate<HelpRequest> searchPredicate = request -> {
            if (currentSearchText == null || currentSearchText.isEmpty()) {
                return true;
            }
            String lowerCaseFilter = currentSearchText.toLowerCase();
            return request.getTitle().toLowerCase().contains(lowerCaseFilter);
        };

        Predicate<HelpRequest> statusPredicate = request -> {
            if (currentStatusFilter == null || currentStatusFilter.equals("All")) {
                return true;
            }

            // Get the request status, handling null cases
            String requestStatus = request.getStatus();
            if (requestStatus == null || requestStatus.isEmpty()) {
                requestStatus = "Pending";
            }

            // Compare the filter value with the request status (case insensitive)
            return currentStatusFilter.equalsIgnoreCase(requestStatus);
        };

        filteredRequestList.setPredicate(searchPredicate.and(statusPredicate));

        // Update status message with search results info
        if (filteredRequestList.isEmpty()) {
            statusMessage.setText("No help requests found matching your criteria.");
        } else {
            statusMessage.setText("");
        }
    }

    /**
     * Load all help requests data from the AdminApprovalController
     */
    private void loadHelpRequestsData() {
        try {
            // Use AdminApprovalController to get all help requests
            List<HelpRequest> requests = adminApprovalController.getAllHelpRequests();

            if (requests.isEmpty()) {
                statusMessage.setText("No help requests found in the system.");
            } else {
                allRequestsList.setAll(requests);
                filteredRequestList = new FilteredList<>(allRequestsList, p -> true);
                helpRequestTableView.setItems(filteredRequestList);
            }
        } catch (Exception e) {
            statusMessage.setText("Error loading help requests: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Set up the actions column with buttons
     */
    private void setupActionsColumn() {
        actionsColumn.setCellFactory(new Callback<TableColumn<HelpRequest, Void>, TableCell<HelpRequest, Void>>() {
            @Override
            public TableCell<HelpRequest, Void> call(TableColumn<HelpRequest, Void> param) {
                return new TableCell<HelpRequest, Void>() {
                    private final Button viewButton = new Button("View Details");
                    private final Button approveButton = new Button("Approve");
                    private final Button rejectButton = new Button("Reject");
                    private final HBox pane = new HBox(5, viewButton, approveButton, rejectButton);

                    {
                        // Configure View Details button
                        viewButton.setStyle("-fx-background-color: #3498db; -fx-text-fill: white;");
                        viewButton.setOnAction(event -> {
                            HelpRequest currentRequest = getTableView().getItems().get(getIndex());
                            handleViewDetails(currentRequest);
                        });

                        // Configure Approve button - Using AdminApprovalController
                        approveButton.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white;");
                        approveButton.setOnAction(event -> {
                            HelpRequest currentRequest = getTableView().getItems().get(getIndex());
                            handleApproveRequest(currentRequest);
                        });

                        // Configure Reject button - Using AdminApprovalController
                        rejectButton.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white;");
                        rejectButton.setOnAction(event -> {
                            HelpRequest currentRequest = getTableView().getItems().get(getIndex());
                            handleRejectRequest(currentRequest);
                        });
                    }

                    @Override
                    protected void updateItem(Void item, boolean empty) {
                        super.updateItem(item, empty);

                        if (empty) {
                            setGraphic(null);
                        } else {
                            HelpRequest request = getTableView().getItems().get(getIndex());

                            // For Pending requests, show all buttons
                            if ("Pending".equalsIgnoreCase(request.getStatus()) || request.getStatus() == null) {
                                pane.getChildren().setAll(viewButton, approveButton, rejectButton);
                            } else {
                                // For non-Pending requests, only show View Details button
                                pane.getChildren().setAll(viewButton);
                            }

                            setGraphic(pane);
                        }
                    }
                };
            }
        });
    }

    /**
     * Handler for Approve button click - uses AdminApprovalController
     *
     * @param request The help request to approve
     */
    private void handleApproveRequest(HelpRequest request) {
        try {
            // Use AdminApprovalController to approve the request
            boolean success = adminApprovalController.approveHelpRequest(request);

            if (success) {
                // Update the status in our list to "Approved"
                request.setStatus("Approved");

                // Refresh the table
                helpRequestTableView.refresh();

                // Show success message
                statusMessage.setText("Help request '" + request.getTitle() + "' has been approved.");
            } else {
                statusMessage.setText("Failed to approve help request.");
            }
        } catch (Exception e) {
            statusMessage.setText("Error approving help request: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Handler for Reject button click - uses AdminApprovalController
     *
     * @param request The help request to reject
     */
    private void handleRejectRequest(HelpRequest request) {
        try {
            // Use AdminApprovalController to reject the request
            boolean success = adminApprovalController.rejectHelpRequest(request);

            if (success) {
                // Update the status in our list to "Rejected"
                request.setStatus("Rejected");

                // Refresh the table
                helpRequestTableView.refresh();

                // Show success message
                statusMessage.setText("Help request '" + request.getTitle() + "' has been rejected.");
            } else {
                statusMessage.setText("Failed to reject help request.");
            }
        } catch (Exception e) {
            statusMessage.setText("Error rejecting help request: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Handler for View Details button click
     *
     * @param request The help request to view details of
     */
    private void handleViewDetails(HelpRequest request) {
        // Currently just display a temporary message
        statusMessage.setText("View details for help request: " + request.getTitle() + " (ID: " + request.getRequestId() + ")");
    }

    /**
     * Handler for search field changes
     */
    @FXML
    public void handleSearch() {
        currentSearchText = searchField.getText();
        applyFilters();

        // Update status message with search results info
        if (filteredRequestList.isEmpty()) {
            statusMessage.setText("No help requests found matching your search criteria.");
        } else {
            statusMessage.setText("");
        }
    }

    /**
     * Handler for clear search button
     */
    @FXML
    public void handleClearSearch() {
        searchField.clear();
        currentSearchText = "";
        applyFilters();
        statusMessage.setText("");
    }

    /**
     * Handler for status filter changes
     */
    @FXML
    public void handleStatusFilter() {
        currentStatusFilter = statusFilterComboBox.getValue();
        applyFilters();

        // Update status message with filter results info
        if (filteredRequestList.isEmpty()) {
            statusMessage.setText("No help requests found with status: " + currentStatusFilter);
        } else {
            statusMessage.setText("");
        }
    }

    /**
     * Handler for back to dashboard button
     */
    @FXML
    public void handleBackToDashboard() {
        try {
            // Load the admin main screen FXML
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/fxml/AdminScreen/AdminMainScreen.fxml"));
            Parent root = loader.load();

            // Get the controller and set the stage and admin
            AdminMainScreenHandler controller = loader.getController();
            controller.setStage(stage);
            controller.setAdmin(admin);

            // Set the scene
            Scene scene = new Scene(root);
            stage.setScene(scene);
            stage.setTitle("Admin Dashboard");
            stage.show();
        } catch (IOException e) {
            statusMessage.setText("Error returning to dashboard: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
