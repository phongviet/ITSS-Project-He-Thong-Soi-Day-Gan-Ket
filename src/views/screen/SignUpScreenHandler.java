package views.screen;

import controller.verification.VerificationController;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class SignUpScreenHandler {

    // Common fields across all sign up screens
    @FXML
    private TextField usernameField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private PasswordField confirmPasswordField;

    @FXML
    private TextField emailField;

    @FXML
    private TextField phoneField;

    @FXML
    private TextField addressField;

    @FXML
    private Label messageLabel;

    @FXML
    private Hyperlink loginLink;

    @FXML
    private Button backButton;

    @FXML
    private Button signUpButton;

    // Role-specific fields - may be null depending on which FXML is loaded
    @FXML
    private TextField fullNameField;

    @FXML
    private TextField organizationNameField;

    @FXML
    private TextField licenseNumberField;

    // Fields for role selection screen - may be null depending on which FXML is loaded
    @FXML
    private RadioButton volunteerRadio;

    @FXML
    private RadioButton organizationRadio;

    @FXML
    private RadioButton personInNeedRadio;

    @FXML
    private Button continueButton;

    private String currentUserType = null;
    private VerificationController verificationController = new VerificationController();

    /**
     * Initialize method called after FXML is loaded
     */
    @FXML
    private void initialize() {
        // Determine which screen we're on based on available fields
        if (volunteerRadio != null && organizationRadio != null && personInNeedRadio != null) {
            // We're on the role selection screen
            currentUserType = "roleSelection";
        } else if (fullNameField != null && organizationNameField == null) {
            // We're on the PersonInNeed screen
            currentUserType = "personInNeed";
        } else if (organizationNameField != null && licenseNumberField != null) {
            // We're on the Organization screen
            currentUserType = "organization";
        } else if (fullNameField != null) {
            // We're on the Volunteer screen
            currentUserType = "volunteer";
        }

        // Setup message label if it exists
        if (messageLabel != null) {
            messageLabel.setText("");
        }
    }

    /**
     * Handle the continue button click from the role selection screen
     */
    @FXML
    private void handleContinue(ActionEvent event) {
        try {
            String fxmlPath;
            if (volunteerRadio.isSelected()) {
                fxmlPath = "/views/fxml/SignUp/VolunteerSignUpScreen.fxml";
            } else if (organizationRadio.isSelected()) {
                fxmlPath = "/views/fxml/SignUp/OrganizationSignUpScreen.fxml";
            } else if (personInNeedRadio.isSelected()) {
                fxmlPath = "/views/fxml/SignUp/PersonInNeedSignUpScreen.fxml";
            } else {
                messageLabel.setText("Please select a role");
                return;
            }

            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent signUpRoot = loader.load();
            Stage stage = (Stage) continueButton.getScene().getWindow();
            stage.setScene(new Scene(signUpRoot));
        } catch (Exception e) {
            messageLabel.setText("Error loading sign-up screen");
            e.printStackTrace();
        }
    }

    /**
     * Go back to role selection
     */
    @FXML
    private void handleBack(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/fxml/SignUp/RoleSelectionScreen.fxml"));
            Parent roleSelectionRoot = loader.load();
            Stage stage = (Stage) backButton.getScene().getWindow();
            stage.setScene(new Scene(roleSelectionRoot));
        } catch (Exception e) {
            messageLabel.setText("Error navigating back to role selection");
            e.printStackTrace();
        }
    }

    /**
     * Handle the sign up button click
     */
    @FXML
    private void handleSignUp(ActionEvent event) {
        // Reset error message
        messageLabel.setText("");
        messageLabel.setStyle("-fx-text-fill: red;");

        // Validate input fields
        if (!validateInputFields()) {
            return;
        }

        // Get common user information
        String username = usernameField.getText().trim();
        String password = passwordField.getText();
        String email = emailField.getText().trim();
        String phone = phoneField.getText().trim();
        String address = addressField.getText().trim();

        boolean registrationSuccess = false;

        try {
            // Register user based on current screen
            if ("volunteer".equals(currentUserType)) {
                String fullName = fullNameField.getText().trim();
                registrationSuccess = verificationController.registerVolunteer(
                        username, password, email, phone, address, fullName);
            }
            else if ("organization".equals(currentUserType)) {
                String orgName = organizationNameField.getText().trim();
                String licenseNumber = licenseNumberField.getText().trim();
                registrationSuccess = verificationController.registerVolunteerOrganization(
                        username, password, email, phone, address, orgName, licenseNumber);
            }
            else if ("personInNeed".equals(currentUserType)) {
                String fullName = fullNameField.getText().trim();
                // We're excluding the needs field, so pass an empty string
                String needs = "";
                registrationSuccess = verificationController.registerPersonInNeed(
                        username, password, email, phone, address, fullName, needs);
            }

            if (registrationSuccess) {
                // Show success message
                messageLabel.setText("Sign up successful! You can now log in.");
                messageLabel.setStyle("-fx-text-fill: green;");

                // Clear fields for another signup or navigate to login
                clearFields();

                // Option: Navigate to login screen after brief delay
                navigateToLoginAfterDelay(2000); // 2 seconds delay
            } else {
                messageLabel.setText("Username already exists or registration failed.");
            }

        } catch (Exception e) {
            messageLabel.setText("Error during sign up: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Navigate to login screen after a delay
     */
    private void navigateToLoginAfterDelay(int delayMs) {
        new Thread(() -> {
            try {
                Thread.sleep(delayMs);
                javafx.application.Platform.runLater(() -> {
                    try {
                        FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/fxml/LogInScreen.fxml"));
                        Parent loginRoot = loader.load();
                        Stage stage = (Stage) loginLink.getScene().getWindow();
                        stage.setScene(new Scene(loginRoot));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }).start();
    }

    /**
     * Validate all input fields
     */
    private boolean validateInputFields() {
        // Check required fields
        if (usernameField.getText().trim().isEmpty()) {
            messageLabel.setText("Username is required");
            return false;
        }

        if (passwordField.getText().isEmpty()) {
            messageLabel.setText("Password is required");
            return false;
        }

        if (!passwordField.getText().equals(confirmPasswordField.getText())) {
            messageLabel.setText("Passwords do not match");
            return false;
        }

        // Validate email format if provided
        if (!emailField.getText().trim().isEmpty() && !isValidEmail(emailField.getText().trim())) {
            messageLabel.setText("Invalid email format");
            return false;
        }

        // Role-specific validations
        if ("volunteer".equals(currentUserType) || "personInNeed".equals(currentUserType)) {
            if (fullNameField.getText().trim().isEmpty()) {
                messageLabel.setText("Full name is required");
                return false;
            }
        } else if ("organization".equals(currentUserType)) {
            if (organizationNameField.getText().trim().isEmpty()) {
                messageLabel.setText("Organization name is required");
                return false;
            }
        }

        return true;
    }

    /**
     * Simple email validation
     */
    private boolean isValidEmail(String email) {
        return email.matches("^[\\w-\\.]+@([\\w-]+\\.)+[\\w-]{2,4}$");
    }

    /**
     * Clear all input fields
     */
    private void clearFields() {
        usernameField.clear();
        passwordField.clear();
        confirmPasswordField.clear();
        emailField.clear();
        phoneField.clear();
        addressField.clear();

        if (fullNameField != null) fullNameField.clear();
        if (organizationNameField != null) organizationNameField.clear();
        if (licenseNumberField != null) licenseNumberField.clear();
    }

    /**
     * Navigate back to login screen
     */
    @FXML
    private void handleLoginLink(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/fxml/LogInScreen.fxml"));
            Parent loginRoot = loader.load();
            Stage stage = (Stage) loginLink.getScene().getWindow();
            stage.setScene(new Scene(loginRoot));
        } catch (Exception e) {
            messageLabel.setText("Error navigating to login screen");
            e.printStackTrace();
        }
    }
}