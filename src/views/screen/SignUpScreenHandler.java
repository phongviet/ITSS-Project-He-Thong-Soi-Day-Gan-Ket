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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
    private TextField cccdField;

    @FXML
    private TextField dateOfBirthField;

    @FXML
    private TextField organizationNameField;

    @FXML
    private TextField licenseNumberField;

    @FXML
    private ComboBox<String> fieldComboBox;

    @FXML
    private TextField representativeField;

    @FXML
    private TextField sponsorField;

    @FXML
    private TextArea infoField;

    @FXML
    private Spinner<Integer> freeHoursSpinner;

    // Volunteer skills checkboxes - updated to match new skill set
    @FXML
    private CheckBox communicationSkillCheckbox;

    @FXML
    private CheckBox firstAidSkillCheckbox;

    @FXML
    private CheckBox educationSkillCheckbox;

    @FXML
    private CheckBox cookingSkillCheckbox;

    @FXML
    private CheckBox drivingSkillCheckbox;

    @FXML
    private CheckBox fundraisingSkillCheckbox;

    @FXML
    private TextField otherSkillsField;

    // Volunteer availability spinners
    @FXML
    private Spinner<Integer> mondaySpinner;

    @FXML
    private Spinner<Integer> tuesdaySpinner;

    @FXML
    private Spinner<Integer> wednesdaySpinner;

    @FXML
    private Spinner<Integer> thursdaySpinner;

    @FXML
    private Spinner<Integer> fridaySpinner;

    @FXML
    private Spinner<Integer> saturdaySpinner;

    @FXML
    private Spinner<Integer> sundaySpinner;

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
            if (communicationSkillCheckbox != null) {
                // We're on the Volunteer screen
                currentUserType = "volunteer";
                initializeSpinners();
            } else {
                // We're on the PersonInNeed screen
                currentUserType = "personInNeed";
            }
        } else if (organizationNameField != null && licenseNumberField != null) {
            // We're on the Organization screen
            currentUserType = "organization";
        }

        // Setup message label if it exists
        if (messageLabel != null) {
            messageLabel.setText("");
        }
    }

    /**
     * Initialize all spinners with proper value factories
     */
    private void initializeSpinners() {
        if (mondaySpinner != null) {
            setupSpinner(mondaySpinner);
            setupSpinner(tuesdaySpinner);
            setupSpinner(wednesdaySpinner);
            setupSpinner(thursdaySpinner);
            setupSpinner(fridaySpinner);
            setupSpinner(saturdaySpinner);
            setupSpinner(sundaySpinner);
        }

        if (freeHoursSpinner != null) {
            SpinnerValueFactory<Integer> valueFactory =
                    new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 168, 0);
            freeHoursSpinner.setValueFactory(valueFactory);
        }
    }

    /**
     * Setup spinner with proper value factory
     */
    private void setupSpinner(Spinner<Integer> spinner) {
        SpinnerValueFactory<Integer> valueFactory =
                new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 24, 0);
        spinner.setValueFactory(valueFactory);
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
                String cccd = cccdField != null ? cccdField.getText().trim() : null;
                String dateOfBirth = dateOfBirthField != null ? dateOfBirthField.getText().trim() : null;

                // Get skills list
                List<String> skills = getSelectedSkills();

                // Get availability map
                Map<String, Integer> availability = getAvailability();

                // Calculate total free hours per week
                int freeHourPerWeek = 0;
                if (freeHoursSpinner != null) {
                    freeHourPerWeek = freeHoursSpinner.getValue();
                } else {
                    for (Integer hours : availability.values()) {
                        freeHourPerWeek += hours;
                    }
                }

                registrationSuccess = verificationController.registerVolunteer(
                        username, password, email, phone, address, fullName,
                        cccd, dateOfBirth, freeHourPerWeek, skills);
            } else if ("organization".equals(currentUserType)) {
                String orgName = organizationNameField.getText().trim();
                String licenseNumber = licenseNumberField.getText().trim();
                String field = fieldComboBox != null ? fieldComboBox.getValue() : "";
                String representative = representativeField != null ? representativeField.getText().trim() : "";
                String sponsor = sponsorField != null ? sponsorField.getText().trim() : "";
                String info = infoField != null ? infoField.getText().trim() : "";

                registrationSuccess = verificationController.registerVolunteerOrganization(
                        username, password, email, phone, address,
                        orgName, licenseNumber, field, representative, sponsor, info);
            } else if ("personInNeed".equals(currentUserType)) {
                String fullName = fullNameField.getText().trim();
                String cccd = cccdField != null ? cccdField.getText().trim() : null;
                String dateOfBirth = dateOfBirthField != null ? dateOfBirthField.getText().trim() : null;

                registrationSuccess = verificationController.registerPersonInNeed(
                        username, password, email, phone, address, fullName, cccd, dateOfBirth);
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
     * Get selected skills from checkboxes and text field
     */
    private List<String> getSelectedSkills() {
        List<String> skills = new ArrayList<>();

        // Check for the new skill checkboxes
        if (communicationSkillCheckbox != null && communicationSkillCheckbox.isSelected()) skills.add("Communication");
        if (firstAidSkillCheckbox != null && firstAidSkillCheckbox.isSelected()) skills.add("First Aid");
        if (educationSkillCheckbox != null && educationSkillCheckbox.isSelected()) skills.add("Education");
        if (cookingSkillCheckbox != null && cookingSkillCheckbox.isSelected()) skills.add("Cooking");
        if (drivingSkillCheckbox != null && drivingSkillCheckbox.isSelected()) skills.add("Driving");
        if (fundraisingSkillCheckbox != null && fundraisingSkillCheckbox.isSelected()) skills.add("Fundraising");

        // Add other skills if specified
        String otherSkills = otherSkillsField != null ? otherSkillsField.getText().trim() : "";
        if (!otherSkills.isEmpty()) {
            String[] otherSkillsList = otherSkills.split(",");
            for (String skill : otherSkillsList) {
                String trimmedSkill = skill.trim();
                if (!trimmedSkill.isEmpty()) {
                    skills.add(trimmedSkill);
                }
            }
        }

        return skills;
    }

    /**
     * Get availability from spinners
     */
    private Map<String, Integer> getAvailability() {
        Map<String, Integer> availability = new HashMap<>();

        if (mondaySpinner != null && mondaySpinner.getValue() > 0) availability.put("Monday", mondaySpinner.getValue());
        if (tuesdaySpinner != null && tuesdaySpinner.getValue() > 0) availability.put("Tuesday", tuesdaySpinner.getValue());
        if (wednesdaySpinner != null && wednesdaySpinner.getValue() > 0) availability.put("Wednesday", wednesdaySpinner.getValue());
        if (thursdaySpinner != null && thursdaySpinner.getValue() > 0) availability.put("Thursday", thursdaySpinner.getValue());
        if (fridaySpinner != null && fridaySpinner.getValue() > 0) availability.put("Friday", fridaySpinner.getValue());
        if (saturdaySpinner != null && saturdaySpinner.getValue() > 0) availability.put("Saturday", saturdaySpinner.getValue());
        if (sundaySpinner != null && sundaySpinner.getValue() > 0) availability.put("Sunday", sundaySpinner.getValue());

        return availability;
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
        if ("volunteer".equals(currentUserType)) {
            if (fullNameField.getText().trim().isEmpty()) {
                messageLabel.setText("Full name is required");
                return false;
            }

            // CCCD validation if field exists
            if (cccdField != null && !cccdField.getText().trim().isEmpty()) {
                String cccd = cccdField.getText().trim();
                if (cccd.length() != 12 || !cccd.matches("\\d+")) {
                    messageLabel.setText("CCCD must be 12 digits");
                    return false;
                }
            }

            // Date validation if field exists
            if (dateOfBirthField != null && !dateOfBirthField.getText().trim().isEmpty()) {
                if (!dateOfBirthField.getText().trim().matches("\\d{4}-\\d{2}-\\d{2}")) {
                    messageLabel.setText("Date of birth must be in format YYYY-MM-DD");
                    return false;
                }
            }

            // Validate at least one skill is selected
            if ((communicationSkillCheckbox == null || !communicationSkillCheckbox.isSelected()) &&
                (firstAidSkillCheckbox == null || !firstAidSkillCheckbox.isSelected()) &&
                (educationSkillCheckbox == null || !educationSkillCheckbox.isSelected()) &&
                (cookingSkillCheckbox == null || !cookingSkillCheckbox.isSelected()) &&
                (drivingSkillCheckbox == null || !drivingSkillCheckbox.isSelected()) &&
                (fundraisingSkillCheckbox == null || !fundraisingSkillCheckbox.isSelected()) &&
                (otherSkillsField == null || otherSkillsField.getText().trim().isEmpty())) {

                messageLabel.setText("Please select at least one skill");
                return false;
            }

            // Validate availability - either free hours or daily availability
            if (freeHoursSpinner != null && freeHoursSpinner.getValue() <= 0 &&
                (mondaySpinner == null || mondaySpinner.getValue() == 0) &&
                (tuesdaySpinner == null || tuesdaySpinner.getValue() == 0) &&
                (wednesdaySpinner == null || wednesdaySpinner.getValue() == 0) &&
                (thursdaySpinner == null || thursdaySpinner.getValue() == 0) &&
                (fridaySpinner == null || fridaySpinner.getValue() == 0) &&
                (saturdaySpinner == null || saturdaySpinner.getValue() == 0) &&
                (sundaySpinner == null || sundaySpinner.getValue() == 0)) {

                messageLabel.setText("Please specify your weekly availability");
                return false;
            }
        } else if ("personInNeed".equals(currentUserType)) {
            if (fullNameField.getText().trim().isEmpty()) {
                messageLabel.setText("Full name is required");
                return false;
            }

            // CCCD validation if field exists
            if (cccdField != null && !cccdField.getText().trim().isEmpty()) {
                String cccd = cccdField.getText().trim();
                if (cccd.length() != 12 || !cccd.matches("\\d+")) {
                    messageLabel.setText("CCCD must be 12 digits");
                    return false;
                }
            }

            // Date validation if field exists
            if (dateOfBirthField != null && !dateOfBirthField.getText().trim().isEmpty()) {
                if (!dateOfBirthField.getText().trim().matches("\\d{4}-\\d{2}-\\d{2}")) {
                    messageLabel.setText("Date of birth must be in format YYYY-MM-DD");
                    return false;
                }
            }
        } else if ("organization".equals(currentUserType)) {
            if (organizationNameField.getText().trim().isEmpty()) {
                messageLabel.setText("Organization name is required");
                return false;
            }

            if (licenseNumberField.getText().trim().isEmpty()) {
                messageLabel.setText("License number is required");
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
        if (cccdField != null) cccdField.clear();
        if (dateOfBirthField != null) dateOfBirthField.clear();
        if (organizationNameField != null) organizationNameField.clear();
        if (licenseNumberField != null) licenseNumberField.clear();
        if (fieldComboBox != null) fieldComboBox.getSelectionModel().clearSelection();
        if (representativeField != null) representativeField.clear();
        if (sponsorField != null) sponsorField.clear();
        if (infoField != null) infoField.clear();

        // Clear Volunteer specific fields
        if ("volunteer".equals(currentUserType)) {
            if (communicationSkillCheckbox != null) communicationSkillCheckbox.setSelected(false);
            if (firstAidSkillCheckbox != null) firstAidSkillCheckbox.setSelected(false);
            if (educationSkillCheckbox != null) educationSkillCheckbox.setSelected(false);
            if (cookingSkillCheckbox != null) cookingSkillCheckbox.setSelected(false);
            if (drivingSkillCheckbox != null) drivingSkillCheckbox.setSelected(false);
            if (fundraisingSkillCheckbox != null) fundraisingSkillCheckbox.setSelected(false);
            if (otherSkillsField != null) otherSkillsField.clear();

            // Reset spinners
            if (freeHoursSpinner != null) {
                freeHoursSpinner.getValueFactory().setValue(0);
            }

            if (mondaySpinner != null) {
                mondaySpinner.getValueFactory().setValue(0);
                tuesdaySpinner.getValueFactory().setValue(0);
                wednesdaySpinner.getValueFactory().setValue(0);
                thursdaySpinner.getValueFactory().setValue(0);
                fridaySpinner.getValueFactory().setValue(0);
                saturdaySpinner.getValueFactory().setValue(0);
                sundaySpinner.getValueFactory().setValue(0);
            }
        }
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

