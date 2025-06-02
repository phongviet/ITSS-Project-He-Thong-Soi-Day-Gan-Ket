package views.screen.AdminScreen;

import controller.AdminStatisticReportController;
import entity.users.Admin;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.chart.PieChart;
import javafx.scene.control.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URL;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.ResourceBundle;

public class StatisticReportScreenHandler implements Initializable {

    // User Stats Section
    @FXML private Label totalUsersLabel;
    @FXML private Label totalVolunteersLabel;
    @FXML private Label totalPeopleInNeedLabel;
    @FXML private Label totalOrgsLabel;
    @FXML private PieChart userDistributionChart;

    // Event Stats Section
    @FXML private Label totalEventsLabel;

    // Help Request Stats Section
    @FXML private Label totalRequestsLabel;
    @FXML private Label pendingRequestsLabel;
    @FXML private Label approvedRequestsLabel;
    @FXML private Label rejectedRequestsLabel;
    @FXML private PieChart requestStatusChart;

    @FXML private Button exportReportButton;
    @FXML private Button backButton;

    private Stage stage;
    private Admin admin;
    private AdminStatisticReportController controller;

    public StatisticReportScreenHandler(Stage stage, Admin admin) {
        this.stage = stage;
        this.admin = admin;
        this.controller = new AdminStatisticReportController();
    }

    // Default constructor needed for FXML loader
    public StatisticReportScreenHandler() {
        this.controller = new AdminStatisticReportController();
    }

    // Setter methods
    public void setStage(Stage stage) {
        this.stage = stage;
    }

    public void setAdmin(Admin admin) {
        this.admin = admin;
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Load data into the screen
        loadAllStatistics();
    }

    private void loadAllStatistics() {
        loadUserStatistics();
        loadEventStatistics();
        loadRequestStatistics();
    }

    private void loadUserStatistics() {
        // Get user statistics from controller
        Map<String, Integer> userStats = controller.getUserStatistics();
        
        // Update summary cards
        int totalUsers = userStats.getOrDefault("totalUsers", 0);
        int volunteers = userStats.getOrDefault("volunteers", 0);
        int peopleInNeed = userStats.getOrDefault("peopleInNeed", 0);
        int organizations = userStats.getOrDefault("organizations", 0);
        
        totalUsersLabel.setText(String.valueOf(totalUsers));
        totalVolunteersLabel.setText(String.valueOf(volunteers));
        totalPeopleInNeedLabel.setText(String.valueOf(peopleInNeed));
        totalOrgsLabel.setText(String.valueOf(organizations));
        
        // Update user distribution pie chart
        ObservableList<PieChart.Data> pieChartData = FXCollections.observableArrayList(
            new PieChart.Data("Volunteers", volunteers),
            new PieChart.Data("People in Need", peopleInNeed),
            new PieChart.Data("Organizations", organizations)
        );
        userDistributionChart.setData(pieChartData);
    }

    private void loadEventStatistics() {
        // Get event statistics from controller
        Map<String, Object> eventStats = controller.getEventStatistics();
        
        // Update summary cards
        int totalEvents = (Integer) eventStats.getOrDefault("totalEvents", 0);

        totalEventsLabel.setText(String.valueOf(totalEvents));
    }

    private void loadRequestStatistics() {
        // Get request statistics from controller
        Map<String, Object> requestStats = controller.getHelpRequestStatistics();
        
        // Update summary cards
        int totalRequests = (Integer) requestStats.getOrDefault("totalRequests", 0);
        int pendingRequests = (Integer) requestStats.getOrDefault("pendingRequests", 0);
        int approvedRequests = (Integer) requestStats.getOrDefault("approvedRequests", 0);
        int rejectedRequests = (Integer) requestStats.getOrDefault("rejectedRequests", 0);
        
        totalRequestsLabel.setText(String.valueOf(totalRequests));
        pendingRequestsLabel.setText(String.valueOf(pendingRequests));
        approvedRequestsLabel.setText(String.valueOf(approvedRequests));
        rejectedRequestsLabel.setText(String.valueOf(rejectedRequests));
        
        // Update request status pie chart
        ObservableList<PieChart.Data> pieChartData = FXCollections.observableArrayList(
            new PieChart.Data("Pending", pendingRequests),
            new PieChart.Data("Approved", approvedRequests),
            new PieChart.Data("Rejected", rejectedRequests)
        );
        requestStatusChart.setData(pieChartData);
    }

    @FXML
    public void handleExportReport() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save Report");
        fileChooser.getExtensionFilters().add(
            new FileChooser.ExtensionFilter("CSV Files", "*.csv")
        );
        
        // Set default filename with current date
        String defaultFilename = "Statistics_Report_" + 
            LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")) + ".csv";
        fileChooser.setInitialFileName(defaultFilename);
        
        File file = fileChooser.showSaveDialog(stage);
        if (file != null) {
            try {
                exportReportToCSV(file);
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Export Successful");
                alert.setHeaderText(null);
                alert.setContentText("Report exported successfully to: " + file.getAbsolutePath());
                alert.showAndWait();
            } catch (IOException e) {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Export Error");
                alert.setHeaderText(null);
                alert.setContentText("Failed to export report: " + e.getMessage());
                alert.showAndWait();
            }
        }
    }
    
    private void exportReportToCSV(File file) throws IOException {
        try (FileWriter writer = new FileWriter(file)) {
            // Write header
            writer.write("Statistical Report - Generated on " + 
                LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")) + "\n\n");
            
            // User Statistics Section
            writer.write("USER STATISTICS\n");
            writer.write("Total Users," + totalUsersLabel.getText() + "\n");
            writer.write("Volunteers," + totalVolunteersLabel.getText() + "\n");
            writer.write("People in Need," + totalPeopleInNeedLabel.getText() + "\n");
            writer.write("Organizations," + totalOrgsLabel.getText() + "\n\n");
            
            // Event Statistics Section
            writer.write("EVENT STATISTICS\n");
            writer.write("Total Events," + totalEventsLabel.getText() + "\n\n");

            // Help Request Statistics Section
            writer.write("HELP REQUEST STATISTICS\n");
            writer.write("Total Requests," + totalRequestsLabel.getText() + "\n");
            writer.write("Pending Requests," + pendingRequestsLabel.getText() + "\n");
            writer.write("Approved Requests," + approvedRequestsLabel.getText() + "\n");
            writer.write("Rejected Requests," + rejectedRequestsLabel.getText() + "\n");
        }
    }

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
            e.printStackTrace();

            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Navigation Error");
            alert.setHeaderText(null);
            alert.setContentText("Error returning to dashboard: " + e.getMessage());
            alert.showAndWait();
        }
    }

    // Data model classes for statistics
    public static class UserStatRow {
        private final String userType;
        private final int count;
        private final int newLastMonth;

        public UserStatRow(String userType, int count, int newLastMonth) {
            this.userType = userType;
            this.count = count;
            this.newLastMonth = newLastMonth;
        }

        public String getUserType() { return userType; }
        public int getCount() { return count; }
        public int getNewLastMonth() { return newLastMonth; }
    }

    public static class EventStatRow {
        private final String eventType;
        private final int count;
        private final double avgParticipants;

        public EventStatRow(String eventType, int count, double avgParticipants) {
            this.eventType = eventType;
            this.count = count;
            this.avgParticipants = avgParticipants;
        }

        public EventStatRow(String eventType, int count) {
            this.eventType = eventType;
            this.count = count;
            this.avgParticipants = 0;
        }

        public String getEventType() { return eventType; }
        public int getCount() { return count; }
        public double getAvgParticipants() { return avgParticipants; }
    }

    public static class RequestStatRow {
        private final String requestType;
        private final int count;

        public RequestStatRow(String requestType, int count) {
            this.requestType = requestType;
            this.count = count;
        }

        public String getRequestType() { return requestType; }
        public int getCount() { return count; }
    }
}
