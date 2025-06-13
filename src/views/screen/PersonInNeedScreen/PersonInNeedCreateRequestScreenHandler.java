package views.screen.PersonInNeedScreen;

import controller.requests.HelpRequestController;
import entity.requests.HelpRequest;
import entity.users.PersonInNeed;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.io.IOException;
import java.time.ZoneId;
import java.util.Date;

public class PersonInNeedCreateRequestScreenHandler {

    @FXML private TextField titleField;
    @FXML private TextArea descriptionArea;
    @FXML private TextField contactField;
    @FXML private DatePicker startDatePicker;
    @FXML private ComboBox<String> emergencyLevelComboBox;
    @FXML private Label statusMessageLabel;
    @FXML private Button backToDashboardButton;

    private Stage stage;
    private PersonInNeed currentUser;
    private HelpRequest requestToEdit = null;
    private final HelpRequestController helpRequestController;

    public PersonInNeedCreateRequestScreenHandler() {
        this.helpRequestController = new HelpRequestController();
    }

    public void setStage(Stage stage) {
        this.stage = stage;
    }

    public void setCurrentUser(PersonInNeed user) {
        this.currentUser = user;
        if (this.currentUser == null) {
            if (backToDashboardButton != null) backToDashboardButton.setDisable(true);
            statusMessageLabel.setText("User not identified. Cannot create requests or go to dashboard.");
            statusMessageLabel.setStyle("-fx-text-fill: red;");
        } else {
            if (backToDashboardButton != null) backToDashboardButton.setDisable(false);
        }
    }

    public void populateForEdit(HelpRequest request) {
        this.requestToEdit = request;
        titleField.setText(request.getTitle());
        descriptionArea.setText(request.getDescription());
        contactField.setText(request.getContact());
        if (request.getStartDate() != null) {
            startDatePicker.setValue(request.getStartDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate());
        } else {
            startDatePicker.setValue(null);
        }
        emergencyLevelComboBox.setValue(request.getEmergencyLevel());
        statusMessageLabel.setText("Editing request: " + request.getTitle());
        statusMessageLabel.setStyle("-fx-text-fill: blue;");
    }

    @FXML
    public void initialize() {
        emergencyLevelComboBox.getItems().addAll("Low", "Normal", "High", "Urgent");
        if (requestToEdit == null) {
            emergencyLevelComboBox.setValue("Normal");
        }
        statusMessageLabel.setText("");
    }

    @FXML
    private void handleSubmitRequest() {
        if (currentUser == null) {
            statusMessageLabel.setText("Error: User not properly identified.");
            statusMessageLabel.setStyle("-fx-text-fill: red;");
            return;
        }

        if (titleField.getText().isEmpty() || descriptionArea.getText().isEmpty() ||
            contactField.getText().isEmpty() || emergencyLevelComboBox.getValue() == null) {
            statusMessageLabel.setText("Please fill in all required fields (Title, Description, Contact, Emergency Level).");
            statusMessageLabel.setStyle("-fx-text-fill: red;");
            return;
        }

        boolean success;
        String successMessage;
        String failureMessage;

        HelpRequest helpRequestData = (requestToEdit != null) ? requestToEdit : new HelpRequest();
        helpRequestData.setTitle(titleField.getText());
        helpRequestData.setDescription(descriptionArea.getText());
        helpRequestData.setContact(contactField.getText());
        if (startDatePicker.getValue() != null) {
            helpRequestData.setStartDate(Date.from(startDatePicker.getValue().atStartOfDay(ZoneId.systemDefault()).toInstant()));
        } else {
            helpRequestData.setStartDate(null);
        }
        helpRequestData.setEmergencyLevel(emergencyLevelComboBox.getValue());

        if (requestToEdit != null) {
            success = helpRequestController.updateHelpRequest(helpRequestData);
            successMessage = "Help request updated successfully!";
            failureMessage = "Failed to update help request. Please try again.";
        } else {
            helpRequestData.setPersonInNeedUsername(currentUser.getUsername());
            helpRequestData.setStatus("Pending");
            success = helpRequestController.createHelpRequest(helpRequestData);
            successMessage = "Help request submitted successfully!";
            failureMessage = "Failed to submit help request. Please try again.";
        }

        if (success) {
            statusMessageLabel.setText(successMessage);
            statusMessageLabel.setStyle("-fx-text-fill: green;");
            if (requestToEdit == null) {
                titleField.clear();
                descriptionArea.clear();
                contactField.clear();
                startDatePicker.setValue(null);
                emergencyLevelComboBox.setValue("Normal");
            } else {
                // For updated request, fields remain as they are. User can navigate away.
            }
        } else {
            statusMessageLabel.setText(failureMessage);
            statusMessageLabel.setStyle("-fx-text-fill: red;");
        }
    }

    @FXML
    private void handleViewMyRequests() {
        if (currentUser == null) return;
        try {
            this.requestToEdit = null;
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/fxml/PersonInNeedScreen/PersonInNeedRequestListScreen.fxml"));
            Parent root = loader.load();
            PersonInNeedRequestListScreenHandler controller = loader.getController();
            controller.setStage(stage);
            controller.setCurrentUser(currentUser);
            Scene scene = new Scene(root);
            stage.setScene(scene);
            stage.setTitle("My Help Requests");
        } catch (IOException e) {
            e.printStackTrace();
            statusMessageLabel.setText("Error loading request list screen: " + e.getMessage());
        }
    }

    @FXML
    private void handleBackToDashboard() {
        if (currentUser == null || stage == null) return;
        try {
            this.requestToEdit = null;
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/fxml/PersonInNeedScreen/PersonInNeedMainScreen.fxml"));
            Parent newRoot = loader.load();
            views.screen.PersonInNeedMainScreenHandler controller = loader.getController();
            controller.setStage(stage);
            controller.setPersonInNeed(currentUser);
            Scene currentScene = stage.getScene();
            if (currentScene != null) currentScene.setRoot(newRoot);
            else stage.setScene(new Scene(newRoot));
            stage.setTitle(currentUser.getName() + "'s Dashboard");
        } catch (IOException e) {
            e.printStackTrace();
            statusMessageLabel.setText("Error loading dashboard screen: " + e.getMessage());
        }
    }
} 