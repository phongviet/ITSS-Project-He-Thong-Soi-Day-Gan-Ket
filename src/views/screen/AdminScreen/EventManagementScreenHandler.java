package views.screen.AdminScreen;

import controller.AdminApprovalController;
import controller.event.EventController;
import entity.events.Event;
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

public class EventManagementScreenHandler implements Initializable {

    @FXML
    private TableView<Event> eventTableView;

    @FXML
    private TableColumn<Event, String> titleColumn;

    @FXML
    private TableColumn<Event, String> organizerColumn;

    @FXML
    private TableColumn<Event, String> startDateColumn;

    @FXML
    private TableColumn<Event, String> endDateColumn;

    @FXML
    private TableColumn<Event, String> statusColumn;

    @FXML
    private TableColumn<Event, Void> actionsColumn;

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
    private EventController eventController;
    private AdminApprovalController adminApprovalController;

    // Store the original unfiltered data
    private ObservableList<Event> allEventsList = FXCollections.observableArrayList();

    // Store the filtered data
    private FilteredList<Event> filteredEventList;

    // Current filters
    private String currentSearchText = "";
    private String currentStatusFilter = "All";

    public EventManagementScreenHandler(Stage stage, Admin admin) {
        this.stage = stage;
        this.admin = admin;
        initializeControllers();
    }

    // Default constructor needed for FXML loader
    public EventManagementScreenHandler() {
    }

    // Setter methods
    public void setStage(Stage stage) {
        this.stage = stage;
    }

    public void setAdmin(Admin admin) {
        this.admin = admin;
        // Initialize the controllers when admin is set
        initializeControllers();

        // After setting admin and initializing controllers, load the events data
        if (eventTableView != null) { // Check if the FXML elements are already initialized
            loadEventsData();
        }
    }

    /**
     * Initialize both controllers with the admin user
     */
    private void initializeControllers() {
        if (admin != null) {
            this.eventController = new EventController();
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

        // Load events data if controllers are initialized
        if (eventController != null) {
            loadEventsData();
        }
    }

    private void setupTableColumns() {
        // Setup title column
        titleColumn.setCellValueFactory(cellData ->
            new SimpleStringProperty(cellData.getValue().getTitle()));

        // Setup organizer column
        organizerColumn.setCellValueFactory(cellData ->
            new SimpleStringProperty(cellData.getValue().getOrganizer()));

        // Setup start date column
        startDateColumn.setCellValueFactory(cellData -> {
            Event event = cellData.getValue();
            if (event.getStartDate() != null) {
                Calendar cal = Calendar.getInstance();
                cal.setTime(event.getStartDate());
                String startDate = String.format("%02d/%02d/%d",
                    cal.get(Calendar.DAY_OF_MONTH),
                    cal.get(Calendar.MONTH) + 1,
                    cal.get(Calendar.YEAR));
                return new SimpleStringProperty(startDate);
            } else {
                return new SimpleStringProperty("N/A");
            }
        });

        // Setup end date column
        endDateColumn.setCellValueFactory(cellData -> {
            Event event = cellData.getValue();
            if (event.getEndDate() != null) {
                Calendar cal = Calendar.getInstance();
                cal.setTime(event.getEndDate());
                String endDate = String.format("%02d/%02d/%d",
                    cal.get(Calendar.DAY_OF_MONTH),
                    cal.get(Calendar.MONTH) + 1,
                    cal.get(Calendar.YEAR));
                return new SimpleStringProperty(endDate);
            } else {
                return new SimpleStringProperty("N/A");
            }
        });

        // Setup status column
        statusColumn.setCellValueFactory(cellData -> {
            Event event = cellData.getValue();

            // Use the status field from the Event class
            String status = event.getStatus();

            // If status is null or empty, fall back to date-based calculation
            if (status == null || status.isEmpty()) {
                if (event.getStartDate() != null && event.getEndDate() != null) {
                    LocalDate startDate = event.getStartDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
                    LocalDate endDate = event.getEndDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
                    LocalDate today = LocalDate.now();

                    if (startDate.isAfter(today)) {
                        status = "Upcoming";
                    } else if (endDate.isBefore(today)) {
                        status = "Completed";
                    } else {
                        status = "Active";
                    }
                } else {
                    status = "Pending";
                }
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
        Predicate<Event> searchPredicate = event -> {
            if (currentSearchText == null || currentSearchText.isEmpty()) {
                return true;
            }
            String lowerCaseFilter = currentSearchText.toLowerCase();
            return event.getTitle().toLowerCase().contains(lowerCaseFilter);
        };

        Predicate<Event> statusPredicate = event -> {
            if (currentStatusFilter == null || currentStatusFilter.equals("All")) {
                return true;
            }

            // Get the event status, handling null cases
            String eventStatus = event.getStatus();
            if (eventStatus == null || eventStatus.isEmpty()) {
                // For events with no status, determine based on dates
                if (event.getStartDate() != null && event.getEndDate() != null) {
                    LocalDate startDate = event.getStartDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
                    LocalDate endDate = event.getEndDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
                    LocalDate today = LocalDate.now();

                    if (startDate.isAfter(today)) {
                        eventStatus = "Upcoming";
                    } else if (endDate.isBefore(today)) {
                        eventStatus = "Completed";
                    } else {
                        eventStatus = "Active";
                    }
                } else {
                    eventStatus = "Pending";
                }
            }

            // Compare the filter value with the event status (case insensitive)
            return currentStatusFilter.equalsIgnoreCase(eventStatus);
        };

        filteredEventList.setPredicate(searchPredicate.and(statusPredicate));

        // Update status message with search results info
        if (filteredEventList.isEmpty()) {
            statusMessage.setText("No events found matching your criteria.");
        } else {
            statusMessage.setText("");
        }
    }

    /**
     * Load all events data from the EventController
     */
    private void loadEventsData() {
        try {
            // Sử dụng EventController để lấy tất cả các sự kiện
            List<Event> events = eventController.getAllEvents();

            if (events.isEmpty()) {
                statusMessage.setText("No events found in the system.");
            } else {
                allEventsList.setAll(events);
                filteredEventList = new FilteredList<>(allEventsList, p -> true);
                eventTableView.setItems(filteredEventList);
            }
        } catch (Exception e) {
            statusMessage.setText("Error loading events: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Set up the actions column with buttons
     */
    private void setupActionsColumn() {
        actionsColumn.setCellFactory(new Callback<TableColumn<Event, Void>, TableCell<Event, Void>>() {
            @Override
            public TableCell<Event, Void> call(TableColumn<Event, Void> param) {
                return new TableCell<Event, Void>() {
                    private final Button viewButton = new Button("View Details");
                    private final Button approveButton = new Button("Approve");
                    private final Button rejectButton = new Button("Reject");
                    private final HBox pane = new HBox(5, viewButton, approveButton, rejectButton);

                    {
                        // Configure View Details button
                        viewButton.setStyle("-fx-background-color: #3498db; -fx-text-fill: white;");
                        viewButton.setOnAction(event -> {
                            Event currentEvent = getTableView().getItems().get(getIndex());
                            handleViewDetails(currentEvent);
                        });

                        // Configure Approve button - Sử dụng AdminApprovalController
                        approveButton.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white;");
                        approveButton.setOnAction(event -> {
                            Event currentEvent = getTableView().getItems().get(getIndex());
                            handleApproveEvent(currentEvent);
                        });

                        // Configure Reject button - Sử dụng AdminApprovalController
                        rejectButton.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white;");
                        rejectButton.setOnAction(event -> {
                            Event currentEvent = getTableView().getItems().get(getIndex());
                            handleRejectEvent(currentEvent);
                        });
                    }

                    @Override
                    protected void updateItem(Void item, boolean empty) {
                        super.updateItem(item, empty);

                        if (empty) {
                            setGraphic(null);
                        } else {
                            Event event = getTableView().getItems().get(getIndex());

                            // For Pending events, show all buttons
                            if ("Pending".equals(event.getStatus())) {
                                pane.getChildren().setAll(viewButton, approveButton, rejectButton);
                            } else {
                                // For non-Pending events, only show View Details button
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
     * @param event The event to approve
     */
    private void handleApproveEvent(Event event) {
        try {
            // Sử dụng AdminApprovalController để phê duyệt sự kiện
            boolean success = adminApprovalController.approveEvent(event.getEventId());

            if (success) {
                // Update the status in our list to "Coming Soon"
                event.setStatus("Coming Soon");

                // Refresh the table
                eventTableView.refresh();

                // Show success message
                statusMessage.setText("Event '" + event.getTitle() + "' has been processed and set to Coming Soon.");
            } else {
                statusMessage.setText("Failed to process event.");
            }
        } catch (Exception e) {
            statusMessage.setText("Error processing event: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Handler for Reject button click - uses AdminApprovalController
     *
     * @param event The event to reject
     */
    private void handleRejectEvent(Event event) {
        try {
            // Call AdminApprovalController to reject the event
            boolean success = adminApprovalController.rejectEvent(event.getEventId());

            if (success) {
                // Update the status in our list to "Rejected"
                event.setStatus("Rejected");

                // Refresh the table
                eventTableView.refresh();

                // Show success message
                statusMessage.setText("Event '" + event.getTitle() + "' has been rejected.");
            } else {
                statusMessage.setText("Failed to reject event.");
            }
        } catch (Exception e) {
            statusMessage.setText("Error rejecting event: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Handler for View Details button click - Opens the event detail view screen
     *
     * @param event The event to view details of
     */
    private void handleViewDetails(Event event) {
        try {
            // Load the admin view event detail screen FXML
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/fxml/AdminScreen/AdminViewEventDetailScreen.fxml"));
            Parent root = loader.load();

            // Get the controller and set the stage, admin, and event
            AdminViewEventDetailScreenHandler controller = loader.getController();
            controller.setStage(stage);
            controller.setAdmin(admin);
            controller.setEvent(event);

            // Set the scene
            Scene scene = new Scene(root);
            stage.setScene(scene);
            stage.setTitle("Event Details: " + event.getTitle());
            stage.show();
        } catch (IOException e) {
            statusMessage.setText("Error opening event details: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Handler for search field changes
     */
    @FXML
    public void handleSearch() {
        currentSearchText = searchField.getText();
        applyFilters();

        // Update status message with search results info
        if (filteredEventList.isEmpty()) {
            statusMessage.setText("No events found matching your search criteria.");
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
        if (filteredEventList.isEmpty()) {
            statusMessage.setText("No events found with status: " + currentStatusFilter);
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
