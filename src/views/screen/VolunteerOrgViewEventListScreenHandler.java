package views.screen;

import controller.event.EventController;
import entity.events.Event;
import entity.users.VolunteerOrganization;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;
import javafx.util.Callback;

import java.io.IOException;
import java.net.URL;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ResourceBundle;
import java.util.function.Predicate;

public class VolunteerOrgViewEventListScreenHandler implements Initializable {

    @FXML
    private TableView<Event> eventTableView;

    @FXML
    private TableColumn<Event, String> titleColumn;

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
    private VolunteerOrganization organization;
    private EventController eventController;
    private DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    // Store the original unfiltered data
    private ObservableList<Event> allEventsList;

    // Store the filtered data
    private FilteredList<Event> filteredEvents;

    // Current filters
    private String currentSearchText = "";
    private String currentStatusFilter = "All";

    public VolunteerOrgViewEventListScreenHandler(Stage stage, VolunteerOrganization organization) {
        this.stage = stage;
        this.organization = organization;
        this.eventController = new EventController();
    }

    // Default constructor needed for FXML loader
    public VolunteerOrgViewEventListScreenHandler() {
        this.eventController = new EventController();
    }

    public void setStage(Stage stage) {
        this.stage = stage;
    }

    public void setOrganization(VolunteerOrganization organization) {
        this.organization = organization;
        loadEventData();
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        setupTableColumns();
        setupFilters();
        statusMessage.setText("");
    }

    private void setupTableColumns() {
        // Setup title column
        titleColumn.setCellValueFactory(cellData ->
            new SimpleStringProperty(cellData.getValue().getTitle()));

        // Setup start date column
        startDateColumn.setCellValueFactory(cellData -> {
            Event event = cellData.getValue();
            String startDate = String.format("%02d/%02d/%d",
                event.getStartDay(),
                event.getStartMonth(),
                event.getStartYear());
            return new SimpleStringProperty(startDate);
        });

        // Setup end date column
        endDateColumn.setCellValueFactory(cellData -> {
            Event event = cellData.getValue();
            String endDate = String.format("%02d/%02d/%d",
                event.getEndDay(),
                event.getEndMonth(),
                event.getEndYear());
            return new SimpleStringProperty(endDate);
        });

        // Setup status column
        statusColumn.setCellValueFactory(cellData -> {
            Event event = cellData.getValue();

            // Use the status field from the Event class
            String status = event.getStatus();

            // If status is null or empty, fall back to date-based calculation
            if (status == null || status.isEmpty()) {
                LocalDate startDate = LocalDate.of(event.getStartYear(), event.getStartMonth(), event.getStartDay());
                LocalDate endDate = LocalDate.of(event.getEndYear(), event.getEndMonth(), event.getEndDay());
                LocalDate today = LocalDate.now();

                if (startDate.isAfter(today)) {
                    status = "Upcoming";
                } else if (endDate.isBefore(today)) {
                    status = "Completed";
                } else {
                    status = "Active";
                }
            } else {
                // Capitalize first letter for display purposes
                status = status.substring(0, 1).toUpperCase() + status.substring(1).toLowerCase();
            }

            return new SimpleStringProperty(status);
        });

        // Setup action column with "View Details" button
        actionsColumn.setCellFactory(new Callback<TableColumn<Event, Void>, TableCell<Event, Void>>() {
            @Override
            public TableCell<Event, Void> call(TableColumn<Event, Void> param) {
                return new TableCell<Event, Void>() {
                    private final Button viewButton = new Button("View Details");

                    {
                        viewButton.setOnAction(event -> {
                            Event data = getTableView().getItems().get(getIndex());
                            handleViewEventDetails(data);
                        });

                        viewButton.setStyle("-fx-background-color: #3498db; -fx-text-fill: white;");
                    }

                    @Override
                    protected void updateItem(Void item, boolean empty) {
                        super.updateItem(item, empty);
                        if (empty) {
                            setGraphic(null);
                        } else {
                            setGraphic(viewButton);
                        }
                    }
                };
            }
        });
    }

    private void setupFilters() {
        // Initialize the status filter combo box
        statusFilterComboBox.setItems(FXCollections.observableArrayList("All", "Pending", "Upcoming", "Completed", "Canceled"));
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
                LocalDate startDate = LocalDate.of(event.getStartYear(), event.getStartMonth(), event.getStartDay());
                LocalDate endDate = LocalDate.of(event.getEndYear(), event.getEndMonth(), event.getEndDay());
                LocalDate today = LocalDate.now();

                if (startDate.isAfter(today)) {
                    eventStatus = "Upcoming";
                } else if (endDate.isBefore(today)) {
                    eventStatus = "Completed";
                } else {
                    eventStatus = "Active";
                }
            }

            // Compare the filter value with the event status (case insensitive)
            return currentStatusFilter.equalsIgnoreCase(eventStatus);
        };

        filteredEvents.setPredicate(searchPredicate.and(statusPredicate));
    }

    private void loadEventData() {
        try {
            if (organization != null) {
                // Get events for this organization using the username as the organizer ID
                List<Event> events = eventController.getEventsByOrganizerId(organization.getUsername());

                if (events.isEmpty()) {
                    statusMessage.setText("No events found for your organization.");
                } else {
                    allEventsList = FXCollections.observableArrayList(events);
                    filteredEvents = new FilteredList<>(allEventsList, p -> true);
                    eventTableView.setItems(filteredEvents);
                }
            } else {
                statusMessage.setText("Organization information not available.");
            }
        } catch (Exception e) {
            statusMessage.setText("Error loading events: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void handleViewEventDetails(Event event) {
        // This would navigate to event details screen
        // To be implemented in future
        statusMessage.setText("View details for event: " + event.getTitle());
    }

    @FXML
    public void handleBackToDashboard() {
        try {
            // Load the organization main dashboard
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/fxml/OrganizationScreen/VolunteerOrgMainScreen.fxml"));
            Parent root = loader.load();

            // Get the controller and pass the organization data
            VolunteerOrgMainScreenHandler controller = loader.getController();
            controller.setStage(stage);
            controller.setOrganization(organization);

            // Set the scene
            Scene scene = new Scene(root);
            stage.setScene(scene);
            stage.setTitle("Volunteer Organization Dashboard");
            stage.show();
        } catch (IOException e) {
            statusMessage.setText("Error returning to dashboard: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    public void handleSearch() {
        currentSearchText = searchField.getText();
        applyFilters();

        // Update status message with search results info
        if (filteredEvents.isEmpty()) {
            statusMessage.setText("No events found matching your search criteria.");
        } else {
            statusMessage.setText("");
        }
    }

    @FXML
    public void handleClearSearch() {
        searchField.clear();
        currentSearchText = "";
        applyFilters();
        statusMessage.setText("");
    }

    @FXML
    public void handleStatusFilter() {
        currentStatusFilter = statusFilterComboBox.getValue();
        applyFilters();

        // Update status message with filter results info
        if (filteredEvents.isEmpty()) {
            statusMessage.setText("No events found with status: " + currentStatusFilter);
        } else {
            statusMessage.setText("");
        }
    }
}
