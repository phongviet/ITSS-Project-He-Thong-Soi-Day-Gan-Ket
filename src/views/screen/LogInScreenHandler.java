package views.screen;

import controller.verification.VerificationController;
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