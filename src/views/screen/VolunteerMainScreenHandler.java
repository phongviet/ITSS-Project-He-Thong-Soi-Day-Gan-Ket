package views.screen;

import entity.users.Volunteer;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

public class VolunteerMainScreenHandler implements Initializable {

    @FXML
    private Button invitationsButton;

    @FXML
    private Button myEventsButton;

    @FXML
    private Label statusMessage;

    private Stage stage;
    private Volunteer volunteer;

    public VolunteerMainScreenHandler(Stage stage, Volunteer volunteer) {
        this.stage = stage;
        this.volunteer = volunteer;
    }

    // Default constructor needed for FXML loader
    public VolunteerMainScreenHandler() {
    }

    // Setter methods
    public void setStage(Stage stage) {
        this.stage = stage;
    }

    public void setVolunteer(Volunteer volunteer) {
        this.volunteer = volunteer;
    }

    public void setStatusMessage(String message) {
        statusMessage.setText(message);
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        // Clear any previous status message
        statusMessage.setText("");
    }

    @FXML
    public void handleViewInvitations() {
        // This will be implemented later
        statusMessage.setText("View Invitations feature will be implemented soon.");
    }

    @FXML
    public void handleViewMyEvents() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/fxml/VolunteerScreen/VolunteerViewMyEventsScreen.fxml")); // ĐƯỜNG DẪN FXML MỚI
            Parent root = loader.load();

            VolunteerViewMyEventsScreenHandler controller = loader.getController();
            controller.setStage(this.stage); // Truyền stage hiện tại
            if (this.volunteer != null) {
                controller.setVolunteer(this.volunteer); // Truyền thông tin Volunteer hiện tại
            } else {
                // Xử lý nếu volunteer là null (có thể là lỗi, nên log lại hoặc hiển thị thông báo)
                System.err.println("Volunteer object is null in VolunteerMainScreenHandler.");
                statusMessage.setText("Error: User data not found. Please try logging in again.");
                // Có thể quay lại màn hình login ở đây nếu cần
                return;
            }

            Scene scene = new Scene(root);
            this.stage.setScene(scene);
            this.stage.setTitle("My Events List"); // Đặt tiêu đề cho cửa sổ mới
            this.stage.show();

        } catch (IOException e) {
            statusMessage.setText("Error opening My Events screen: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    public void handleLogout() {
        try {
            // Load the login FXML
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/fxml/LogInScreen.fxml"));
            Parent root = loader.load();

            // Set the scene
            Scene scene = new Scene(root);
            stage.setScene(scene);
            stage.setTitle("Login");
            stage.show();
        } catch (IOException e) {
            statusMessage.setText("Error logging out: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
