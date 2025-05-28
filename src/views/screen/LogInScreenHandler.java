package views.screen;

import controller.verification.VerificationController;
import entity.users.Admin;
import entity.users.Volunteer;
import entity.users.VolunteerOrganization;
import entity.users.PersonInNeed;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

import java.io.IOException;

public class LogInScreenHandler {

    @FXML
    private TextField usernameField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private Button loginButton;

    @FXML
    private Button signUpButton;

    @FXML
    private Label messageLabel;

    private VerificationController verificationController = new VerificationController();

    @FXML
    private void handleLogin(ActionEvent event) {
        String username = usernameField.getText();
        String password = passwordField.getText();

        // Validate input fields
        if (username.isEmpty() || password.isEmpty()) {
            messageLabel.setText("Username and password cannot be empty");
            messageLabel.setTextFill(Color.RED);
            return;
        }

        if (verificationController.checkInfo(username, password)) {
            // Get user type from database
            String userType = verificationController.getUserType(username);
            Stage stage = (Stage) loginButton.getScene().getWindow();

            try {
                if ("Admin".equals(userType)) {
                    // Load the FXML file with its declared controller for Admin main screen
                    FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/fxml/AdminScreen/AdminMainScreen.fxml"));
                    Parent root = loader.load();

                    // Get the controller that was created by the FXML loader
                    AdminMainScreenHandler controller = loader.getController();

                    // Set the admin data in the controller
                    Admin admin = verificationController.getAdmin(username);
                    controller.setAdmin(admin);
                    controller.setStage(stage);

                    // Set the scene
                    stage.setScene(new Scene(root));
                    stage.setTitle("Admin Dashboard");
                    stage.show();
                } else if ("VolunteerOrganization".equals(userType)) {
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
                } else if ("Volunteer".equals(userType)) {
                    // Load the FXML file with its declared controller for Volunteer main screen
                    FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/fxml/VolunteerScreen/VolunteerMainScreen.fxml"));
                    Parent root = loader.load();

                    // Get the controller that was created by the FXML loader
                    VolunteerMainScreenHandler controller = loader.getController();

                    // Set the volunteer data in the controller
                    Volunteer volunteer = verificationController.getVolunteer(username);
                    controller.setVolunteer(volunteer);
                    controller.setStage(stage);

                    // Set the scene
                    stage.setScene(new Scene(root));
                    stage.setTitle("Volunteer Dashboard");
                    stage.show();
                } else if ("PersonInNeed".equals(userType)) {
                    // Load the FXML file with its declared controller for PersonInNeed main screen
                    FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/fxml/PersonInNeedScreen/PersonInNeedMainScreen.fxml"));
                    Parent root = loader.load();

                    // Get the controller that was created by the FXML loader
                    PersonInNeedMainScreenHandler controller = loader.getController();

                    // Set the person in need data in the controller
                    PersonInNeed personInNeed = verificationController.getPersonInNeed(username);
                    controller.setPersonInNeed(personInNeed);
                    controller.setStage(stage);

                    // Set the scene
                    stage.setScene(new Scene(root));
                    stage.setTitle("Person In Need Dashboard");
                    stage.show();
                } else if (userType != null) {
                    // Handle other user types (Volunteer, PersonInNeed)
                    messageLabel.setText("Login successful, but no specific UI for " + userType + " yet.");
                    messageLabel.setTextFill(Color.GREEN);
                } else {
                    // User exists in SystemUser but not in any specific role table
                    messageLabel.setText("User account exists but has no assigned role.");
                    messageLabel.setTextFill(Color.ORANGE);
                }
            } catch (Exception e) {
                messageLabel.setText("Error loading dashboard: " + e.getMessage());
                messageLabel.setTextFill(Color.RED);
                e.printStackTrace();
            }
        } else {
            messageLabel.setText("Login failed! Invalid username or password.");
            messageLabel.setTextFill(Color.RED);
        }
    }

    @FXML
    private void handleSignUp(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/fxml/SignUp/RoleSelectionScreen.fxml"));
            Parent signUpRoot = loader.load();
            Stage stage = (Stage) signUpButton.getScene().getWindow();
            stage.setScene(new Scene(signUpRoot));
        } catch (IOException e) {
            messageLabel.setText("Error loading sign up screen: " + e.getMessage());
            messageLabel.setTextFill(Color.RED);
            e.printStackTrace();
        }
    }
}
