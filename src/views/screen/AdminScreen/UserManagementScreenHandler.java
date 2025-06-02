package views.screen.AdminScreen;

import controller.UserController;
import entity.users.Admin;
import entity.users.SystemUser;
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
import java.util.List;
import java.util.ResourceBundle;
import java.util.function.Predicate;

public class UserManagementScreenHandler implements Initializable {

    @FXML
    private TableView<SystemUser> userTableView;

    @FXML
    private TableColumn<SystemUser, String> usernameColumn;

    @FXML
    private TableColumn<SystemUser, String> emailColumn;

    @FXML
    private TableColumn<SystemUser, String> phoneColumn;

    @FXML
    private TableColumn<SystemUser, String> addressColumn;

    @FXML
    private TableColumn<SystemUser, String> roleColumn;

    @FXML
    private TextField searchField;

    @FXML
    private ComboBox<String> roleFilterComboBox;

    @FXML
    private Label statusMessage;

    @FXML
    private Button backButton;

    private Stage stage;
    private Admin admin;
    private UserController userController;

    // Store the original unfiltered data
    private ObservableList<SystemUser> allUsersList = FXCollections.observableArrayList();

    // Store the filtered data
    private FilteredList<SystemUser> filteredUserList;

    // Current filters
    private String currentSearchText = "";
    private String currentRoleFilter = "All";

    public UserManagementScreenHandler(Stage stage, Admin admin) {
        this.stage = stage;
        this.admin = admin;
        initializeControllers();
    }

    // Default constructor needed for FXML loader
    public UserManagementScreenHandler() {
    }

    // Setter methods
    public void setStage(Stage stage) {
        this.stage = stage;
    }

    public void setAdmin(Admin admin) {
        this.admin = admin;
        // Initialize the controllers when admin is set
        initializeControllers();

        // After setting admin and initializing controllers, load the users data
        if (userTableView != null) { // Check if the FXML elements are already initialized
            loadUsersData();
        }
    }

    /**
     * Initialize the user controller
     */
    private void initializeControllers() {
        if (admin != null) {
            this.userController = new UserController();
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

        // Load users data if controllers are initialized
        if (userController != null) {
            loadUsersData();
        }
    }

    private void setupTableColumns() {
        // Setup username column
        usernameColumn.setCellValueFactory(cellData ->
            new SimpleStringProperty(cellData.getValue().getUsername()));

        // Setup email column
        emailColumn.setCellValueFactory(cellData -> {
            String email = cellData.getValue().getEmail();
            return new SimpleStringProperty(email != null ? email : "N/A");
        });

        // Setup phone column
        phoneColumn.setCellValueFactory(cellData -> {
            String phone = cellData.getValue().getPhone();
            return new SimpleStringProperty(phone != null ? phone : "N/A");
        });

        // Setup address column
        addressColumn.setCellValueFactory(cellData -> {
            String address = cellData.getValue().getAddress();
            return new SimpleStringProperty(address != null ? address : "N/A");
        });

        // Setup role column
        roleColumn.setCellValueFactory(cellData -> {
            SystemUser user = cellData.getValue();
            String role = "";

            if (user instanceof Admin) {
                role = "Admin";
            } else if (user.getClass().getSimpleName().equals("Volunteer")) {
                role = "Volunteer";
            } else if (user.getClass().getSimpleName().equals("PersonInNeed")) {
                role = "Person In Need";
            } else if (user.getClass().getSimpleName().equals("VolunteerOrganization")) {
                role = "Organization";
            }

            return new SimpleStringProperty(role);
        });
    }

    private void setupFilters() {
        // Initialize the role filter combo box
        roleFilterComboBox.setValue("All");

        // Add listeners to the search field and role filter combo box
        searchField.textProperty().addListener((observable, oldValue, newValue) -> {
            currentSearchText = newValue;
            applyFilters();
        });

        roleFilterComboBox.valueProperty().addListener((observable, oldValue, newValue) -> {
            currentRoleFilter = newValue;
            applyFilters();
        });
    }

    /**
     * Apply both search and role filters
     */
    private void applyFilters() {
        Predicate<SystemUser> searchPredicate = user -> {
            if (currentSearchText == null || currentSearchText.isEmpty()) {
                return true;
            }
            String lowerCaseFilter = currentSearchText.toLowerCase();
            return user.getUsername().toLowerCase().contains(lowerCaseFilter);
        };

        Predicate<SystemUser> rolePredicate = user -> {
            if (currentRoleFilter == null || currentRoleFilter.equals("All")) {
                return true;
            }

            String userRole = "";
            if (user instanceof Admin) {
                userRole = "Admin";
            } else if (user.getClass().getSimpleName().equals("Volunteer")) {
                userRole = "Volunteer";
            } else if (user.getClass().getSimpleName().equals("PersonInNeed")) {
                userRole = "PersonInNeed";
            } else if (user.getClass().getSimpleName().equals("VolunteerOrganization")) {
                userRole = "VolunteerOrganization";
            }

            return currentRoleFilter.equals(userRole);
        };

        filteredUserList.setPredicate(searchPredicate.and(rolePredicate));

        // Update status message with search results info
        if (filteredUserList.isEmpty()) {
            statusMessage.setText("No users found matching your criteria.");
        } else {
            statusMessage.setText("");
        }
    }

    /**
     * Load all users data from the UserController
     */
    private void loadUsersData() {
        try {
            // Use UserController to get all users
            List<SystemUser> users = userController.getAllUsers();

            if (users.isEmpty()) {
                statusMessage.setText("No users found in the system.");
            } else {
                allUsersList.setAll(users);
                filteredUserList = new FilteredList<>(allUsersList, p -> true);
                userTableView.setItems(filteredUserList);
            }
        } catch (Exception e) {
            statusMessage.setText("Error loading users: " + e.getMessage());
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
        if (filteredUserList.isEmpty()) {
            statusMessage.setText("No users found matching your search criteria.");
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
     * Handler for role filter changes
     */
    @FXML
    public void handleRoleFilter() {
        currentRoleFilter = roleFilterComboBox.getValue();
        applyFilters();

        // Update status message with filter results info
        if (filteredUserList.isEmpty()) {
            statusMessage.setText("No users found with role: " + currentRoleFilter);
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
