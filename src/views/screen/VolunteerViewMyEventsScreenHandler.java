package views.screen;

import controller.event.EventController;
import entity.events.Event; // Bạn cần tạo lớp Event.java trước
import entity.users.Volunteer;
import javafx.beans.property.SimpleIntegerProperty;
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
import javafx.util.Callback; // Cần import Callback

import java.io.IOException;
import java.net.URL;
import java.text.SimpleDateFormat; // Sử dụng SimpleDateFormat cho nhất quán
import java.util.Date;
import java.util.List;
import java.util.ResourceBundle;
import java.util.function.Predicate;

// Giả sử bạn có một lớp EventParticipantDetails để chứa thông tin kết hợp
// giữa Event và thông tin tham gia của TNV (hours, rating).
// Nếu không, bạn cần điều chỉnh cách lấy và hiển thị dữ liệu này.
// Tạm thời, chúng ta sẽ cố gắng hiển thị dựa trên đối tượng Event và truy vấn thêm.

public class VolunteerViewMyEventsScreenHandler implements Initializable {

    @FXML
    private TableView<Event> eventTableView; // Sẽ hiển thị Event, nhưng cần lấy thêm thông tin từ EventParticipants

    @FXML
    private TableColumn<Event, String> titleColumn;

    @FXML
    private TableColumn<Event, String> startDateColumn;

    @FXML
    private TableColumn<Event, String> endDateColumn;

    @FXML
    private TableColumn<Event, String> statusColumn; // Trạng thái tham gia của TNV

    @FXML
    private TableColumn<Event, String> hoursParticipatedColumn; // THAY ĐỔI kiểu thành String để hiển thị

    @FXML
    private TableColumn<Event, String> ratingByOrgColumn; // THAY ĐỔI kiểu thành String để hiển thị

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
    private Volunteer volunteer; // TNV hiện tại
    private EventController eventController;
    private SimpleDateFormat dateFormatter = new SimpleDateFormat("dd/MM/yyyy");


    private ObservableList<Event> allMyEventsList;
    private FilteredList<Event> filteredMyEvents;

    private String currentSearchText = "";
    private String currentStatusFilter = "All";

    // Constructor mặc định
    public VolunteerViewMyEventsScreenHandler() {
        this.eventController = new EventController();
    }

    // Setter để truyền dữ liệu từ màn hình trước
    public void setStage(Stage stage) {
        this.stage = stage;
    }

    public void setVolunteer(Volunteer volunteer) {
        this.volunteer = volunteer;
        loadMyEventData(); // Tải dữ liệu khi có thông tin TNV
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        setupTableColumns();
        setupFilters();
        statusMessage.setText("");
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

        // Cột Status (Trạng thái tham gia của TNV với sự kiện)
        // Cần logic để lấy trạng thái từ bảng EventParticipants hoặc Notification
        statusColumn.setCellValueFactory(cellData -> {
            // Tạm thời để "Registered" hoặc lấy từ Event.status nếu phù hợp
            // TODO: Cần lấy acceptStatus từ Notification hoặc trạng thái từ EventParticipants
            Event event = cellData.getValue();
            // Đây là trạng thái chung của sự kiện, không phải trạng thái tham gia của TNV
            String eventGlobalStatus = event.getStatus() != null ? event.getStatus() : "Unknown";
            // Bạn cần một cách để lấy trạng thái tham gia cụ thể của TNV này với sự kiện này.
            // Ví dụ: eventController.getVolunteerEventStatus(volunteer.getUsername(), event.getEventId());
            return new SimpleStringProperty("Registered (" + eventGlobalStatus + ")"); // Placeholder
        });

        // Cột Hours Participated
        hoursParticipatedColumn.setCellValueFactory(cellData -> {
            Event event = cellData.getValue();
            // TODO: Cần lấy hoursParticipated từ bảng EventParticipants cho TNV này và Event này
            // Ví dụ: int hours = eventController.getHoursParticipated(volunteer.getUsername(), event.getEventId());
            // return new SimpleStringProperty(hours > 0 ? String.valueOf(hours) : "N/A");
            return new SimpleStringProperty("N/A"); // Placeholder
        });

        // Cột Org Rating
        ratingByOrgColumn.setCellValueFactory(cellData -> {
            Event event = cellData.getValue();
            // TODO: Cần lấy ratingByOrg từ bảng EventParticipants cho TNV này và Event này
            // Ví dụ: int rating = eventController.getRatingByOrg(volunteer.getUsername(), event.getEventId());
            // return new SimpleStringProperty(rating > 0 ? String.valueOf(rating) + "/5" : "N/A");
            return new SimpleStringProperty("N/A"); // Placeholder
        });


        actionsColumn.setCellFactory(param -> new TableCell<Event, Void>() {
            private final Button viewButton = new Button("View Details");

            {
                viewButton.setOnAction(event -> {
                    Event selectedEvent = getTableView().getItems().get(getIndex());
                    handleViewEventDetails(selectedEvent);
                });
                viewButton.setStyle("-fx-background-color: #9b59b6; -fx-text-fill: white;");
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : viewButton);
            }
        });
    }

    private void setupFilters() {
        // Các trạng thái có thể liên quan đến TNV
        statusFilterComboBox.setItems(FXCollections.observableArrayList(
                "All", "Registered", "Upcoming", "Ongoing", "Completed", "Canceled"
        ));
        statusFilterComboBox.setValue("All");

        searchField.textProperty().addListener((obs, oldVal, newVal) -> {
            currentSearchText = newVal;
            applyFilters();
        });

        statusFilterComboBox.valueProperty().addListener((obs, oldVal, newVal) -> {
            currentStatusFilter = newVal;
            applyFilters();
        });
    }

    private void applyFilters() {
        Predicate<Event> searchPredicate = event -> {
            if (currentSearchText == null || currentSearchText.isEmpty()) {
                return true;
            }
            return event.getTitle().toLowerCase().contains(currentSearchText.toLowerCase());
        };

        Predicate<Event> statusPredicate = event -> {
            if (currentStatusFilter == null || currentStatusFilter.equals("All")) {
                return true;
            }
            // TODO: Cần logic lọc theo trạng thái tham gia của TNV với sự kiện
            // String volunteerEventStatus = getVolunteerEventStatusLogic(event, volunteer.getUsername());
            // return currentStatusFilter.equalsIgnoreCase(volunteerEventStatus);
            return true; // Placeholder, cần implement logic lọc theo status của TNV
        };

        if (allMyEventsList != null) {
            filteredMyEvents.setPredicate(searchPredicate.and(statusPredicate));
        }
    }

    private void loadMyEventData() {
        if (volunteer == null) {
            statusMessage.setText("Volunteer data not available.");
            return;
        }
        try {
            // TODO: Cần một phương thức trong EventController để lấy các sự kiện TNV đã tham gia/đăng ký
            // List<Event> events = eventController.getEventsForVolunteer(volunteer.getUsername());
            // Tạm thời, để test, ta có thể lấy tất cả sự kiện rồi lọc thủ công (không tối ưu)
            List<Event> events = eventController.getAllEvents(); // Đây chỉ là placeholder, CẦN THAY ĐỔI
                                                                // để chỉ lấy sự kiện của TNV này

            if (events.isEmpty()) {
                statusMessage.setText("You are not participating in any events yet.");
                allMyEventsList = FXCollections.observableArrayList();
            } else {
                statusMessage.setText("");
                allMyEventsList = FXCollections.observableArrayList(events);
            }
            filteredMyEvents = new FilteredList<>(allMyEventsList, p -> true);
            eventTableView.setItems(filteredMyEvents);
            applyFilters(); // Áp dụng filter ban đầu (All)

        } catch (Exception e) {
            statusMessage.setText("Error loading your events: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void handleViewEventDetails(Event event) {
        statusMessage.setText("View details for event: " + event.getTitle() + " (To be implemented).");
        // Logic điều hướng đến màn hình chi tiết sự kiện sẽ ở đây
    }

    @FXML
    public void handleBackToDashboard() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/fxml/VolunteerMainScreen.fxml"));
            Parent root = loader.load();

            VolunteerMainScreenHandler controller = loader.getController();
            controller.setStage(stage);
            // Quan trọng: Truyền lại đối tượng Volunteer khi quay về dashboard
            if (this.volunteer != null) {
                controller.setVolunteer(this.volunteer);
            } else {
                // Xử lý trường hợp volunteer là null, có thể là lỗi hoặc cần đăng nhập lại
                System.err.println("Volunteer object is null when returning to dashboard.");
                // Có thể chuyển về màn hình login thay vì dashboard nếu volunteer là null
                 FXMLLoader loginLoader = new FXMLLoader(getClass().getResource("/views/fxml/LogInScreen.fxml"));
                 Parent loginRoot = loginLoader.load();
                 Scene loginScene = new Scene(loginRoot);
                 stage.setScene(loginScene);
                 stage.setTitle("Login");
                 stage.show();
                 return;
            }

            Scene scene = new Scene(root);
            stage.setScene(scene);
            stage.setTitle("Volunteer Dashboard");
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
        statusMessage.setText(filteredMyEvents.isEmpty() ? "No events found matching your criteria." : "");
    }

    @FXML
    public void handleClearSearch() {
        searchField.clear();
        // currentSearchText = ""; // không cần vì listener của textProperty sẽ tự cập nhật
        // applyFilters(); // không cần vì listener của textProperty sẽ tự gọi
        statusMessage.setText("");
    }

    @FXML
    public void handleStatusFilter() {
        // currentStatusFilter = statusFilterComboBox.getValue(); // không cần vì listener của valueProperty sẽ tự cập nhật
        // applyFilters(); // không cần vì listener của valueProperty sẽ tự gọi
        statusMessage.setText(filteredMyEvents.isEmpty() ? "No events found with status: " + currentStatusFilter : "");
    }
}