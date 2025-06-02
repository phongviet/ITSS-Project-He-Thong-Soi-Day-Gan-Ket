package views.screen;

import controller.event.EventController;
import javafx.scene.paint.Color;
import controller.notification.NotificationController; // CẦN CONTROLLER MỚI
import controller.verification.VerificationController; // Để lấy tên tổ chức
import entity.events.Event;
import entity.users.Volunteer;
import entity.users.VolunteerOrganization; // Để lấy tên
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

import java.io.IOException;
import java.net.URL;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.ResourceBundle;
import java.util.function.Predicate;

public class VolunteerSuggestedEventsScreenHandler implements Initializable {

    @FXML private TableView<Event> eventTableView;
    @FXML private TableColumn<Event, String> titleColumn;
    @FXML private TableColumn<Event, String> hostOrganizationColumn;
    @FXML private TableColumn<Event, String> startDateColumn;
    @FXML private TableColumn<Event, String> emergencyLevelColumn;
    @FXML private TableColumn<Event, String> statusColumn; // Trạng thái chung của Event
    @FXML private TableColumn<Event, Void> actionsColumn;
    @FXML private TextField searchField;
    // THÊM @FXML CHO COMBOBOX MỚI
    @FXML private ComboBox<String> registrationStatusFilterComboBox;
    @FXML private Label statusMessage;

    private Stage stage;
    private Volunteer volunteer;
    private EventController eventController;
    private NotificationController notificationController; // Controller mới cho Notification
    private VerificationController verificationController; // Để lấy thông tin tổ chức

    private ObservableList<Event> allSuggestedEventsList;
    private FilteredList<Event> filteredSuggestedEvents;
    private SimpleDateFormat dateFormatter = new SimpleDateFormat("dd/MM/yyyy");

    private String currentSearchText = "";
    private String currentRegistrationStatusFilter = "All Events"; // Giá trị mặc định mới

    public VolunteerSuggestedEventsScreenHandler() {
        this.eventController = new EventController();
        this.notificationController = new NotificationController(); // Khởi tạo
        this.verificationController = new VerificationController(); // Khởi tạo
    }

    public void setStage(Stage stage) {
        this.stage = stage;
    }

    public void setVolunteer(Volunteer volunteer) {
        this.volunteer = volunteer;
        if (this.volunteer != null) {
            loadSuggestedEvents();
        } else {
            statusMessage.setText("Error: Volunteer data is not available.");
        }
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        setupTableColumns();
        //setupSearchFilter();
        setupFilters();
        statusMessage.setText("");
    }
    
    private void setupTableColumns() {
        // ... (cấu hình các cột titleColumn, hostOrganizationColumn, startDateColumn, emergencyLevelColumn, statusColumn như cũ)
        titleColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getTitle()));
        startDateColumn.setCellValueFactory(cellData -> {
            Date date = cellData.getValue().getStartDate();
            return new SimpleStringProperty(date != null ? dateFormatter.format(date) : "N/A");
        });
        emergencyLevelColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getEmergencyLevel()));
        statusColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getStatus()));

        hostOrganizationColumn.setCellValueFactory(cellData -> {
            String organizerUsername = cellData.getValue().getOrganizer();
            if (organizerUsername != null) {
                VolunteerOrganization org = verificationController.getVolunteerOrganization(organizerUsername);
                if (org != null && org.getOrganizationName() != null) {
                    return new SimpleStringProperty(org.getOrganizationName());
                }
            }
            return new SimpleStringProperty("Unknown");
        });


        actionsColumn.setCellFactory(param -> new TableCell<Event, Void>() {
            private final Button viewButton = new Button("View Details");
            private final Button registerButton = new Button(); // Text và style sẽ được đặt trong updateItem
            private final HBox pane = new HBox(5, viewButton, registerButton);

            {
                pane.setAlignment(Pos.CENTER);
                viewButton.setOnAction(event -> {
                    Event selectedEvent = getTableView().getItems().get(getIndex());
                    handleViewEventDetails(selectedEvent); // Giữ nguyên
                });
                viewButton.setStyle("-fx-background-color: #3498db; -fx-text-fill: white;");
                // Không set style cố định cho registerButton ở đây nữa
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || volunteer == null) { // Thêm kiểm tra volunteer null
                    setGraphic(null);
                } else {
                    setGraphic(pane);
                    Event currentEvent = getTableView().getItems().get(getIndex());
                    
                    // Mặc định cho phép đăng ký nếu sự kiện còn "đăng ký được"
                    boolean canEventBeRegistered = "Approved".equalsIgnoreCase(currentEvent.getStatus()) ||
                                                 "Coming Soon".equalsIgnoreCase(currentEvent.getStatus()) ||
                                                 "Pending".equalsIgnoreCase(currentEvent.getStatus()); // Trạng thái sự kiện cho phép đăng ký
                    
                    registerButton.setDisable(!canEventBeRegistered); // Vô hiệu hóa nếu sự kiện không cho đăng ký

                    try {
                        String notificationStatus = notificationController.getVolunteerNotificationStatusForEvent(
                                volunteer.getUsername(), currentEvent.getEventId());

                        if (notificationStatus != null) {
                            if ("Pending".equalsIgnoreCase(notificationStatus)) {
                                registerButton.setText("Waiting Approval");
                                registerButton.setStyle("-fx-background-color: #f39c12; -fx-text-fill: white;"); // Vàng
                                registerButton.setDisable(true); // Đã gửi yêu cầu, không cho bấm nữa
                            } else if ("Approved".equalsIgnoreCase(notificationStatus) || "Registered".equalsIgnoreCase(notificationStatus)) {
                                registerButton.setText("Approved");
                                registerButton.setStyle("-fx-background-color: #2ecc71; -fx-text-fill: white;"); // Xanh lá
                                registerButton.setDisable(true); // Đã được duyệt, không cho bấm nữa
                            } else { // Ví dụ: Rejected, Canceled, hoặc trạng thái khác
                                // Nếu bị rejected/canceled, có thể cho đăng ký lại hoặc không tùy logic
                                registerButton.setText("Register");
                                registerButton.setStyle("-fx-background-color: #3498db; -fx-text-fill: white;"); // Xanh dương
                                // registerButton.setDisable(!canEventBeRegistered); // Giữ nguyên logic disable dựa trên trạng thái sự kiện
                            }
                        } else {
                            // Chưa có notification nào -> chưa đăng ký
                            registerButton.setText("Register");
                            registerButton.setStyle("-fx-background-color: #3498db; -fx-text-fill: white;"); // Xanh dương
                            // registerButton.setDisable(!canEventBeRegistered); // Giữ nguyên logic disable dựa trên trạng thái sự kiện
                        }
                    } catch (SQLException e) {
                        System.err.println("Error checking notification status in cell factory: " + e.getMessage());
                        registerButton.setText("Register"); // Trạng thái mặc định nếu lỗi
                        registerButton.setStyle("-fx-background-color: #3498db; -fx-text-fill: white;");
                        // registerButton.setDisable(!canEventBeRegistered);
                    }
                    
                    // Action cho nút registerButton
                    registerButton.setOnAction(actionEvent -> {
                         Event eventToRegister = getTableView().getItems().get(getIndex());
                         handleRegisterForEvent(eventToRegister, registerButton); // Truyền cả nút để cập nhật sau khi đăng ký
                    });
                }
            }
        });
    }

    private void setupFilters() { // Đổi tên hàm này để bao gồm cả hai filter
        // Search Field Listener
        searchField.textProperty().addListener((obs, oldVal, newVal) -> {
            currentSearchText = newVal != null ? newVal.toLowerCase() : "";
            applyFilters();
        });

        // Registration Status Filter ComboBox Listener
        // Các giá trị phải khớp với FXML
        registrationStatusFilterComboBox.setItems(FXCollections.observableArrayList(
                "All Events", "Not Registered", "Waiting for Approval", "Approved"
        ));
        registrationStatusFilterComboBox.setValue("All Events"); // Giá trị mặc định

        registrationStatusFilterComboBox.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                currentRegistrationStatusFilter = newVal;
                applyFilters();
            }
        });
    }

    private void applyFilters() {
        if (allSuggestedEventsList == null || volunteer == null) { // Thêm kiểm tra volunteer
             if (filteredSuggestedEvents != null) filteredSuggestedEvents.setPredicate(p->false); // Xóa bảng nếu dữ liệu chưa sẵn sàng
            return;
        }

        Predicate<Event> combinedPredicate = event -> {
            boolean titleMatch = true;
            if (!currentSearchText.isEmpty() && event.getTitle() != null) {
                titleMatch = event.getTitle().toLowerCase().contains(currentSearchText);
            }

            boolean registrationStatusMatch = true;
            if (!"All Events".equals(currentRegistrationStatusFilter)) {
                try {
                    String actualStatus = notificationController.getVolunteerNotificationStatusForEvent(
                            volunteer.getUsername(), event.getEventId());

                    if ("Not Registered".equals(currentRegistrationStatusFilter)) {
                        registrationStatusMatch = (actualStatus == null);
                    } else if ("Waiting for Approval".equals(currentRegistrationStatusFilter)) {
                        registrationStatusMatch = "Pending".equalsIgnoreCase(actualStatus);
                    } else if ("Approved".equals(currentRegistrationStatusFilter)) {
                        registrationStatusMatch = "Approved".equalsIgnoreCase(actualStatus) ||
                                                 "Registered".equalsIgnoreCase(actualStatus); // Coi Registered là Approved
                    }
                    // Bạn có thể thêm các trường hợp khác như "Rejected", "Canceled" nếu có trong ComboBox
                } catch (SQLException e) {
                    System.err.println("Error applying registration status filter for event " + event.getEventId() + ": " + e.getMessage());
                    registrationStatusMatch = false; // Nếu lỗi thì coi như không khớp để tránh hiển thị sai
                }
            }
            return titleMatch && registrationStatusMatch;
        };

        filteredSuggestedEvents.setPredicate(combinedPredicate);
        updateStatusMessageAfterFilter();
    }
    
    private void updateStatusMessageAfterFilter() {
        if (filteredSuggestedEvents == null) return;
        if (filteredSuggestedEvents.isEmpty()) {
            if (!currentSearchText.isEmpty() && !"All Events".equals(currentRegistrationStatusFilter)) {
                statusMessage.setText("No events match title '" + currentSearchText + "' and status '" + currentRegistrationStatusFilter + "'.");
            } else if (!currentSearchText.isEmpty()) {
                statusMessage.setText("No events match title '" + currentSearchText + "'.");
            } else if (!"All Events".equals(currentRegistrationStatusFilter)) {
                statusMessage.setText("No events found with status: '" + currentRegistrationStatusFilter + "'.");
            } else {
                // Trường hợp này không nên xảy ra nếu allSuggestedEventsList không rỗng và filter là "All"
                statusMessage.setText("No suggested events available.");
            }
        } else {
            statusMessage.setText("");
        }
    }

    private void loadSuggestedEvents() {
        if (volunteer == null || volunteer.getUsername() == null) {
            statusMessage.setText("Cannot load suggestions: Volunteer information is missing.");
            eventTableView.setItems(FXCollections.observableArrayList());
            return;
        }
        try {
            List<Event> suggestedEvents = eventController.getSuggestedEventsForVolunteer(this.volunteer);
            if (suggestedEvents.isEmpty()) {
                statusMessage.setText("No suitable events found for you at the moment.");
                allSuggestedEventsList = FXCollections.observableArrayList();
            } else {
                statusMessage.setText(""); // Xóa thông báo nếu có sự kiện
                allSuggestedEventsList = FXCollections.observableArrayList(suggestedEvents);
            }
            filteredSuggestedEvents = new FilteredList<>(allSuggestedEventsList, p -> true);
            eventTableView.setItems(filteredSuggestedEvents);
            applyFilters();
        } catch (Exception e) {
            statusMessage.setText("Error loading suggested events: " + e.getMessage());
            e.printStackTrace();
            eventTableView.setItems(FXCollections.observableArrayList());
        }
    }

    private void handleViewEventDetails(Event eventToView) { // Tham số là Event
        if (eventToView == null) {
            statusMessage.setText("Cannot view details: Event data is missing.");
            statusMessage.setStyle("-fx-text-fill: red;");
            return;
        }
        try {
            // Đảm bảo đường dẫn FXML đúng
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/fxml/VolunteerScreen/VolunteerSuggestedEventDetailScreen.fxml"));
            Parent root = loader.load();

            VolunteerSuggestedEventDetailScreenHandler detailController = loader.getController();
            detailController.setStage(this.stage);
            detailController.setVolunteer(this.volunteer); // Truyền Volunteer hiện tại
            detailController.setEventToDisplay(eventToView);  // Truyền đối tượng Event

            Scene scene = new Scene(root);
            this.stage.setScene(scene);
            this.stage.setTitle("Event Details: " + eventToView.getTitle());
            // Không cần stage.show() nếu stage đã hiển thị, setScene là đủ.
            // Nếu đây là cửa sổ mới hoàn toàn thì cần stage.show()

        } catch (IOException e) {
            statusMessage.setText("Error opening event details: " + e.getMessage());
            statusMessage.setStyle("-fx-text-fill: red;");
            e.printStackTrace();
        }
    }

    private void handleRegisterForEvent(Event event, Button registerButton) {
        if (volunteer == null || event == null) {
            statusMessage.setText("Cannot register: Missing information.");
            return;
        }
        // Gọi NotificationController để tạo thông báo đăng ký
        boolean success = notificationController.createRegistrationNotification(event.getEventId(), volunteer.getUsername());

        if (success) {
            statusMessage.setText("Successfully applied to join: " + event.getTitle() + ". Waiting for approval.");
            statusMessage.setStyle("-fx-text-fill: #27ae60;"); // Màu xanh
            registerButton.setDisable(true); // Vô hiệu hóa nút sau khi đăng ký
            registerButton.setText("Applied");
        } else {
            statusMessage.setText("Already applied for event: " + event.getTitle());
            statusMessage.setStyle("-fx-text-fill: #e74c3c;"); // Màu đỏ
        }
    }


    @FXML
    public void handleBackToDashboard() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/fxml/VolunteerScreen/VolunteerMainScreen.fxml"));
            Parent root = loader.load();
            VolunteerMainScreenHandler controller = loader.getController();
            controller.setStage(this.stage);
            controller.setVolunteer(this.volunteer);
            Scene scene = new Scene(root);
            this.stage.setScene(scene);
            this.stage.setTitle("Volunteer Dashboard");
        } catch (IOException e) {
            statusMessage.setText("Error returning to dashboard: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    @FXML // ĐẢM BẢO CÓ ANNOTATION NÀY
    public void handleRegistrationStatusFilter() {
        // Vì chúng ta đã sử dụng valueProperty().addListener() trong setupFilters()
        // để tự động gọi applyFilters() khi giá trị ComboBox thay đổi,
        // phương thức onAction này có thể không thực sự cần làm gì nhiều.
        // Tuy nhiên, nó vẫn cần tồn tại để FXML không báo lỗi.
        // Bạn có thể thêm log ở đây để kiểm tra xem nó có được gọi không nếu muốn.
        // System.out.println("handleRegistrationStatusFilter called - ComboBox value: " + registrationStatusFilterComboBox.getValue());
        
        // applyFilters(); // Không cần gọi lại ở đây nếu listener đã gọi
        // updateStatusMessageAfterFilter(); // Không cần gọi lại ở đây nếu listener đã gọi
    }

    @FXML
    public void handleSearch() { /* Listener đã xử lý */ }

    @FXML
    public void handleClearSearch() {
        searchField.clear();
    }
}