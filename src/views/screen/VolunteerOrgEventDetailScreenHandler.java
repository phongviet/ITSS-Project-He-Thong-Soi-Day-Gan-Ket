package views.screen;

import entity.events.Event;
import entity.users.VolunteerOrganization;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ResourceBundle;

public class VolunteerOrgEventDetailScreenHandler implements Initializable {

    @FXML private Label titleLabel;
    @FXML private Label startDateLabel;
    @FXML private Label endDateLabel;
    @FXML private Label statusLabel;
    @FXML private Label emergencyLevelLabel;
    @FXML private TextArea descriptionArea;
    @FXML private Label requestIdLabel;   // Thêm dòng này

    private Stage stage;
    private VolunteerOrganization organization;
    private Event event;
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");

    public VolunteerOrgEventDetailScreenHandler(Stage stage, VolunteerOrganization org) {
        this.stage = stage;
        this.organization = org;
    }

    public VolunteerOrgEventDetailScreenHandler() {
        // constructor mặc định
    }

    public void setStage(Stage stage) {
        this.stage = stage;
    }

    public void setOrganization(VolunteerOrganization org) {
        this.organization = org;
    }

    public void setEvent(Event event) {
        this.event = event;
        showEventDetails();
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        // Chỉ cấu hình ban đầu, dữ liệu nằm ở setEvent(...)
    }

    private void showEventDetails() {
        if (event == null) return;

        titleLabel.setText(event.getTitle() != null ? event.getTitle() : "N/A");
        if (event.getStartDate() != null) {
            startDateLabel.setText(DATE_FORMAT.format(event.getStartDate()));
        } else {
            startDateLabel.setText("N/A");
        }
        if (event.getEndDate() != null) {
            endDateLabel.setText(DATE_FORMAT.format(event.getEndDate()));
        } else {
            endDateLabel.setText("N/A");
        }

        statusLabel.setText(event.getStatus() != null ? event.getStatus() : "N/A");
        emergencyLevelLabel.setText(event.getEmergencyLevel() != null ? event.getEmergencyLevel() : "N/A");
        descriptionArea.setText(event.getDescription() != null ? event.getDescription() : "");

        // Hiển thị requestId thay vì needer
        if (event.getRequestId() != null) {
            requestIdLabel.setText(event.getRequestId().toString());
        } else {
            requestIdLabel.setText("N/A");
        }
    }

    @FXML
    public void handleBack() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(
                "/views/fxml/OrganizationScreen/VolunteerOrgViewEventListScreen.fxml"));
            Parent root = loader.load();

            VolunteerOrgViewEventListScreenHandler controller = loader.getController();
            controller.setStage(stage);
            controller.setOrganization(organization);

            Scene scene = new Scene(root);
            stage.setScene(scene);
            stage.setTitle("My Events List");
            stage.show();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
