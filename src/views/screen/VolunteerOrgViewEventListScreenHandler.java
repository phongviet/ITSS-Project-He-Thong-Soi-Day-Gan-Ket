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
import javafx.geometry.Pos;
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
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.ResourceBundle;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class VolunteerOrgViewEventListScreenHandler implements Initializable {

    // Event Status Constants
    private static final String STATUS_PENDING = "Pending";
    private static final String STATUS_APPROVED = "Approved";
    private static final String STATUS_REJECTED = "Rejected";
    private static final String STATUS_COMING_SOON = "Coming Soon";
    private static final String STATUS_DONE = "Done"; // Giả sử "Completed" trong DB là "Done" khi hiển thị hoặc xử lý
    private static final String STATUS_CANCELED = "Canceled";
    private static final String STATUS_COMPLETED = "Completed"; // Thường dùng trong DB

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
        actionsColumn.setCellFactory(new Callback<TableColumn<Event, Void>, TableCell<Event, Void>>() {
            @Override
            public TableCell<Event, Void> call(TableColumn<Event, Void> param) {
                return new TableCell<Event, Void>() {
                    private final Button viewDetailsButton = new Button("View Details");
                    private final Button rateButton = new Button("Đánh giá TNV");
                    private final ComboBox<String> statusComboBox = new ComboBox<>();
                    private final HBox pane = new HBox(5);

                    {
                        pane.setAlignment(Pos.CENTER_LEFT);
                        
                        viewDetailsButton.setStyle("-fx-background-color: #3498db; -fx-text-fill: white;");
                        rateButton.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white;");
                        statusComboBox.setPrefWidth(130);

                        viewDetailsButton.setOnAction(event -> {
                            Event data = getTableView().getItems().get(getIndex());
                            handleViewEventDetails(data);
                        });

                        rateButton.setOnAction(event -> {
                            Event data = getTableView().getItems().get(getIndex());
                            handleRateVolunteers(data);
                        });

                        statusComboBox.setOnAction(event -> {
                            Event currentEvent = getTableView().getItems().get(getIndex());
                            String selectedStatus = statusComboBox.getValue();
                            if (selectedStatus != null && !selectedStatus.equals(currentEvent.getStatus())) {
                                handleChangeEventStatus(currentEvent, selectedStatus);
                            }
                        });
                    }

                    @Override
                    protected void updateItem(Void item, boolean empty) {
                        super.updateItem(item, empty);
                        if (empty) {
                            setGraphic(null);
                        } else {
                            pane.getChildren().clear();
                            Event event = getTableView().getItems().get(getIndex());
                            String currentStatus = event.getStatus();

                            pane.getChildren().add(viewDetailsButton);

                            if (STATUS_DONE.equalsIgnoreCase(currentStatus) || STATUS_COMPLETED.equalsIgnoreCase(currentStatus)) {
                                pane.getChildren().add(rateButton);
                            }

                            List<String> possibleStatuses = getPossibleStatusTransitions(currentStatus);
                            if (!possibleStatuses.isEmpty()) {
                                statusComboBox.setItems(FXCollections.observableArrayList(possibleStatuses));
                                statusComboBox.setValue(currentStatus);
                                pane.getChildren().add(statusComboBox);
                            }
                            
                            if (!pane.getChildren().isEmpty()){
                                setGraphic(pane);
                            } else {
                                setGraphic(null);
                            }
                        }
                    }
                };
            }
        });
    }

    private List<String> getPossibleStatusTransitions(String currentStatus) {
        if (currentStatus == null) return Arrays.asList();
        // Org can change status if Admin has approved it.
        // Cannot change from Pending (Admin's job), Rejected, Done, Canceled (final states for Org)
        switch (currentStatus.toLowerCase()) {
            case "approved":
                return Arrays.asList(STATUS_APPROVED, STATUS_COMING_SOON, STATUS_CANCELED);
            case "coming soon":
                return Arrays.asList(STATUS_COMING_SOON, STATUS_DONE, STATUS_CANCELED);
            default:
                return Arrays.asList(); // No transitions for Pending, Rejected, Done, Canceled by Org
        }
    }

    private void handleChangeEventStatus(Event event, String newStatus) {
        if (eventController.updateEventStatus(event.getEventId(), newStatus)) {
            statusMessage.setText("Event '" + event.getTitle() + "' status updated to " + newStatus);
            loadEventData(); // Refresh table
        } else {
            statusMessage.setText("Failed to update status for event '" + event.getTitle() + "'.");
            // Optionally revert ComboBox selection or show error dialog
            loadEventData(); // still refresh to show original state from DB
        }
    }

    private void handleRateVolunteers(Event event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/fxml/OrganizationScreen/RateEventVolunteersScreen.fxml"));
            Parent root = loader.load();
            RateEventVolunteersScreenHandler controller = loader.getController();
            controller.setStage(stage);
            controller.setOrganization(organization);
            controller.setEventToRate(event);

            Scene scene = new Scene(root, 1024, 768);
            stage.setScene(scene);
            stage.setTitle("Rate Volunteers for: " + event.getTitle());
            stage.show();
        } catch (IOException e) {
            statusMessage.setText("Error loading rating screen: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void setupFilters() {
        // Initialize the status filter combo box
        statusFilterComboBox.setItems(FXCollections.observableArrayList("All", STATUS_PENDING, STATUS_APPROVED, STATUS_COMING_SOON, STATUS_COMPLETED, STATUS_CANCELED ));
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
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(
                "/views/fxml/OrganizationScreen/VolunteerOrgEventDetailScreen.fxml"));
            Parent root = loader.load();

            VolunteerOrgEventDetailScreenHandler controller = loader.getController();
            controller.setStage(stage);
            controller.setOrganization(organization);
            controller.setEvent(event);  // truyền Event cần hiển thị

            Scene scene = new Scene(root);
            stage.setScene(scene);
            stage.setTitle("Event Details");
            stage.show();

        } catch (IOException e) {
            statusMessage.setText("Error loading event detail screen: " + e.getMessage());
            e.printStackTrace();
        }
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

