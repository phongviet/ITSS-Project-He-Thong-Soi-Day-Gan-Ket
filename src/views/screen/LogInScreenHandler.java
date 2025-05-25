package views.screen;

import controller.verification.VerificationController;
import entity.users.VolunteerOrganization;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.control.Label;
import javafx.stage.Stage;

public class LogInScreenHandler {

    @FXML
    private TextField usernameField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private Button loginButton;

    @FXML
    private Label messageLabel;

    private VerificationController verificationController = new VerificationController();

    @FXML
    private void handleLogin(ActionEvent event) {
        String username = usernameField.getText();
        String password = passwordField.getText();

        if (verificationController.checkInfo(username, password)) {
            messageLabel.setText("Login successful!");

            // Determine user type and navigate to the appropriate screen
            try {
                // Get user type from database
                String userType = verificationController.getUserType(username);
                Stage stage = (Stage) loginButton.getScene().getWindow();

                if ("VolunteerOrganization".equals(userType)) {
                    // Load the FXML file with its declared controller
                    FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/fxml/OrganizationScreen/VolunteerOrgMainScreen.fxml"));
                    Parent root = loader.load();
                    
                    // Get the controller that was created by the FXML loader
                    VolunteerOrgMainScreenHandler controller = loader.getController();
                    
                    // Set the organization data in the controller
                    VolunteerOrganization organization = verificationController.getVolunteerOrganization(username);
                    controller.setOrganization(organization);
                    controller.setStage(stage);
                    
                    // Set the scene
                    stage.setScene(new Scene(root));
                    stage.setTitle("Volunteer Organization Dashboard");
                    stage.show();
                } else {
                    // Handle other user types (Volunteer, PersonInNeed, etc.)
                    messageLabel.setText("Login successful, but no specific UI for " + userType + " yet.");
                }
            } catch (Exception e) {
                messageLabel.setText("Error loading dashboard: " + e.getMessage());
                e.printStackTrace();
            }
        } else {
            messageLabel.setText("Login failed!");
        }
    }
    @FXML
    private Button signUpButton;

    @FXML
    private void handleSignUp(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/fxml/SignUp/RoleSelectionScreen.fxml"));
            Parent signUpRoot = loader.load();
            Stage stage = (Stage) signUpButton.getScene().getWindow();
            stage.setScene(new Scene(signUpRoot));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}