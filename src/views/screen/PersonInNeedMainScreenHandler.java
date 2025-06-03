package views.screen;

import entity.users.PersonInNeed;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;
import views.screen.PersonInNeedScreen.PersonInNeedRequestListScreenHandler;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

public class PersonInNeedMainScreenHandler implements Initializable {

    @FXML
    private Button requestHelpButton;

    @FXML
    private Button myRequestsButton;

    @FXML
    private Label statusMessage;

    private Stage stage;
    private PersonInNeed personInNeed;

    public PersonInNeedMainScreenHandler(Stage stage, PersonInNeed personInNeed) {
        this.stage = stage;
        this.personInNeed = personInNeed;
    }

    // Default constructor needed for FXML loader
    public PersonInNeedMainScreenHandler() {
    }

    // Setter methods
    public void setStage(Stage stage) {
        this.stage = stage;
    }

    public void setPersonInNeed(PersonInNeed personInNeed) {
        this.personInNeed = personInNeed;
        if (this.personInNeed != null) {
            myRequestsButton.setDisable(false);
            requestHelpButton.setDisable(false);
        } else {
            myRequestsButton.setDisable(true);
            requestHelpButton.setDisable(true);
        }
    }

    public void setStatusMessage(String message) {
        if (statusMessage != null) {
            statusMessage.setText(message);
        }
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        statusMessage.setText("");
        if (personInNeed == null) {
            myRequestsButton.setDisable(true);
            requestHelpButton.setDisable(true);
        }
    }

    @FXML
    public void handleRequestHelp() {
        if (stage == null || personInNeed == null) {
            setStatusMessage("Error: Stage or User not properly initialized.");
            return;
        }
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/fxml/PersonInNeedScreen/PersonInNeedCreateRequestScreen.fxml"));
            Parent newRoot = loader.load();
            views.screen.PersonInNeedScreen.PersonInNeedCreateRequestScreenHandler controller = loader.getController();
            controller.setStage(stage);
            controller.setCurrentUser(personInNeed);

            Scene currentScene = stage.getScene();
            if (currentScene != null) {
                currentScene.setRoot(newRoot);
            } else {
                stage.setScene(new Scene(newRoot)); // Fallback
            }
            stage.setTitle("Create Help Request");
        } catch (IOException e) {
            e.printStackTrace();
            setStatusMessage("Error loading 'Request Help' screen: " + e.getMessage());
        }
    }

    @FXML
    public void handleViewMyRequests() {
        if (stage == null || personInNeed == null) {
            setStatusMessage("Error: Stage or User not properly initialized. Cannot show requests.");
            if (statusMessage != null) {
                statusMessage.setStyle("-fx-text-fill: red;");
            }
            return;
        }
        try {
            java.net.URL fxmlUrl = getClass().getResource("/views/fxml/PersonInNeedScreen/PersonInNeedRequestListScreen.fxml");
            if (fxmlUrl == null) {
                setStatusMessage("Error: FXML file for 'My Requests' not found.");
                if (statusMessage != null) statusMessage.setStyle("-fx-text-fill: red;");
                System.err.println("FXML Resource not found: /views/fxml/PersonInNeedScreen/PersonInNeedRequestListScreen.fxml");
                return;
            }
            FXMLLoader loader = new FXMLLoader(fxmlUrl);
            Parent newRoot = loader.load();
            PersonInNeedRequestListScreenHandler controller = loader.getController();
            controller.setStage(stage);
            controller.setCurrentUser(personInNeed);

            Scene currentScene = stage.getScene();
            if (currentScene != null) {
                currentScene.setRoot(newRoot);
            } else {
                stage.setScene(new Scene(newRoot)); // Fallback
            }
            stage.setTitle("My Help Requests");
        } catch (IOException e) {
            e.printStackTrace();
            setStatusMessage("Error loading 'My Requests' screen: " + e.getMessage());
            if (statusMessage != null) statusMessage.setStyle("-fx-text-fill: red;");
        }
    }

    @FXML
    public void handleLogout() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/fxml/LogInScreen.fxml"));
            Parent root = loader.load();
            // For logout, it's common to create a new scene or ensure the stage is reset appropriately.
            Scene currentScene = stage.getScene();
            if (currentScene != null) {
                currentScene.setRoot(root); // Or new Scene(root) if a full reset is desired
            } else {
                stage.setScene(new Scene(root, 1024, 768));
            }
            stage.setTitle("Login");
            stage.show(); // Ensure stage is shown, especially if creating new scene instances often.
        } catch (IOException e) {
            setStatusMessage("Error logging out: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
