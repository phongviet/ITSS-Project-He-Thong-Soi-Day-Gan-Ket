package views.screen;

import controller.UserController;
import controller.event.EventController;
import entity.events.EventParticipantDetails; // SỬ DỤNG LỚP MỚI
import entity.users.Volunteer;
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
import java.io.IOException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.ResourceBundle;
import java.util.function.Predicate;

public class VolunteerViewMyEventsScreenHandler implements Initializable {

    @FXML
    private TableView<EventParticipantDetails> eventTableView; // THAY ĐỔI KIỂU DỮ LIỆU

    @FXML
    private TableColumn<EventParticipantDetails, String> titleColumn;

    @FXML
    private TableColumn<EventParticipantDetails, String> startDateColumn;

    @FXML
    private TableColumn<EventParticipantDetails, String> endDateColumn;

    @FXML
    private TableColumn<EventParticipantDetails, String> statusColumn; // Trạng thái tham gia của TNV

    @FXML
    private TableColumn<EventParticipantDetails, String> hoursParticipatedColumn;

    @FXML
    private TableColumn<EventParticipantDetails, String> ratingByOrgColumn;

    @FXML
    private TableColumn<EventParticipantDetails, Void> actionsColumn;

    @FXML
    private TextField searchField;

    @FXML
    private ComboBox<String> statusFilterComboBox;

    @FXML
    private Label statusMessage;

    // Nút Back không cần @FXML nếu không có logic đặc biệt trong initialize
    // @FXML private Button backButton;

    private Stage stage;
    private Volunteer volunteer;
    private EventController eventController;
    private UserController userController;
    private SimpleDateFormat dateFormatter = new SimpleDateFormat("dd/MM/yyyy");

    private ObservableList<EventParticipantDetails> allMyParticipationDetailsList;
    private FilteredList<EventParticipantDetails> filteredMyParticipationDetails;

    private String currentSearchText = "";
    private String currentStatusFilter = "All";

    public VolunteerViewMyEventsScreenHandler() {
        this.eventController = new EventController();
        this.userController = new UserController();
    }

    public void setStage(Stage stage) {
        this.stage = stage;
    }

    public void setVolunteer(Volunteer volunteer) {
        this.volunteer = volunteer;
        if (this.volunteer != null) {
            loadMyEventParticipationData();
        } else {
            statusMessage.setText("Error: Volunteer data is not available.");
        }
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        setupTableColumns();
        setupFilters();
        statusMessage.setText("");
        // Không cần load data ở đây vì volunteer có thể chưa được set
    }

    private void setupTableColumns() {
        titleColumn.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getTitle()));

        startDateColumn.setCellValueFactory(cellData -> {
            Date date = cellData.getValue().getStartDate();
            return new SimpleStringProperty(date != null ? dateFormatter.format(date) : "N/A");
        });

        endDateColumn.setCellValueFactory(cellData -> {
            Date date = cellData.getValue().getEndDate();
            return new SimpleStringProperty(date != null ? dateFormatter.format(date) : "N/A");
        });

        // Trạng thái tham gia của TNV với sự kiện này
        statusColumn.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getVolunteerParticipationStatus()));

        hoursParticipatedColumn.setCellValueFactory(cellData -> {
            Integer hours = cellData.getValue().getHoursParticipated();
            return new SimpleStringProperty(hours != null && hours > 0 ? hours.toString() : "N/A");
        });

        ratingByOrgColumn.setCellValueFactory(cellData -> {
            Integer rating = cellData.getValue().getRatingByOrg();
            return new SimpleStringProperty(rating != null && rating > 0 ? rating.toString() : "N/A");
        });

        actionsColumn.setCellFactory(param -> new TableCell<EventParticipantDetails, Void>() {
            private final Button viewButton = new Button("View Details");
            {
                viewButton.setOnAction(event -> {
                    EventParticipantDetails selectedData = getTableView().getItems().get(getIndex());
                    handleViewEventDetails(selectedData);
                });
                viewButton.setStyle("-fx-background-color: #9b59b6; -fx-text-fill: white;"); // Màu nút
            }
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : viewButton);
            }
        });
    }

    private void setupFilters() {
        statusFilterComboBox.setItems(FXCollections.observableArrayList(
                "All", "Registered", "Pending Invite", "Attended", "Completed", "Canceled" // Các trạng thái tham gia của TNV
        ));
        statusFilterComboBox.setValue("All");

        searchField.textProperty().addListener((obs, oldVal, newVal) -> {
            currentSearchText = newVal != null ? newVal.toLowerCase() : "";
            applyFilters();
        });

        statusFilterComboBox.valueProperty().addListener((obs, oldVal, newVal) -> {
            currentStatusFilter = newVal;
            applyFilters();
        });
    }

    private void applyFilters() {
        if (allMyParticipationDetailsList == null) return;

        Predicate<EventParticipantDetails> searchPredicate = details -> {
            if (currentSearchText.isEmpty()) {
                return true;
            }
            return details.getTitle() != null && details.getTitle().toLowerCase().contains(currentSearchText);
        };

        Predicate<EventParticipantDetails> statusPredicate = details -> {
            if (currentStatusFilter == null || currentStatusFilter.equals("All")) {
                return true;
            }
            // Lọc theo trạng thái tham gia của TNV
            return details.getVolunteerParticipationStatus() != null &&
                   details.getVolunteerParticipationStatus().equalsIgnoreCase(currentStatusFilter);
        };

        filteredMyParticipationDetails.setPredicate(searchPredicate.and(statusPredicate));
        statusMessage.setText(filteredMyParticipationDetails.isEmpty() ? "No events match your criteria." : "");
    }

    private void loadMyEventParticipationData() {
        if (volunteer == null || volunteer.getUsername() == null) {
            statusMessage.setText("Cannot load events: Volunteer information is missing.");
            eventTableView.setItems(FXCollections.observableArrayList()); // Xóa bảng nếu không có volunteer
            return;
        }
        try {
            List<EventParticipantDetails> detailsList = userController.getEventParticipationDetailsForVolunteer(volunteer.getUsername());
            if (detailsList.isEmpty()) {
                statusMessage.setText("You are not currently participating in any events.");
                allMyParticipationDetailsList = FXCollections.observableArrayList();
            } else {
                statusMessage.setText("");
                allMyParticipationDetailsList = FXCollections.observableArrayList(detailsList);
            }
            filteredMyParticipationDetails = new FilteredList<>(allMyParticipationDetailsList, p -> true);
            eventTableView.setItems(filteredMyParticipationDetails);
            applyFilters(); // Áp dụng filter ban đầu

        } catch (Exception e) {
            statusMessage.setText("Error loading your event participations: " + e.getMessage());
            e.printStackTrace();
            eventTableView.setItems(FXCollections.observableArrayList());
        }
    }

    @FXML
    public void handleBackToDashboard() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/fxml/VolunteerScreen/VolunteerMainScreen.fxml"));
            Parent root = loader.load();
            VolunteerMainScreenHandler controller = loader.getController();
            controller.setStage(this.stage);
            controller.setVolunteer(this.volunteer); // Quan trọng: truyền lại volunteer
            Scene scene = new Scene(root);
            this.stage.setScene(scene);
            this.stage.setTitle("Volunteer Dashboard");
        } catch (IOException e) {
            statusMessage.setText("Error returning to dashboard: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void handleViewEventDetails(EventParticipantDetails details) { // THAY ĐỔI THAM SỐ
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/fxml/VolunteerScreen/VolunteerViewEventDetailScreen.fxml"));
            Parent root = loader.load();

            VolunteerViewEventDetailScreenHandler detailController = loader.getController();
            detailController.setStage(this.stage);
            detailController.setVolunteer(this.volunteer);
            detailController.setEventDetails(details); // TRUYỀN EventParticipantDetails

            Scene scene = new Scene(root);
            this.stage.setScene(scene);
            this.stage.setTitle("Event Details: " + details.getTitle()); // Cập nhật tiêu đề
            this.stage.show();

        } catch (IOException e) {
            statusMessage.setText("Error opening event details: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    public void handleSearch() {
        // Listener đã xử lý, hàm này có thể trống hoặc dùng để cập nhật UI phụ nếu cần
        // currentSearchText = searchField.getText().toLowerCase();
        // applyFilters();
    }

    @FXML
    public void handleClearSearch() {
        searchField.clear(); // Listener sẽ tự động gọi applyFilters
        // currentSearchText = "";
        // applyFilters();
    }

    @FXML
    public void handleStatusFilter() {
        // Listener đã xử lý, hàm này có thể trống
        // currentStatusFilter = statusFilterComboBox.getValue();
        // applyFilters();
    }
}