package entity.requests;

import java.util.Date;

/**
 * HelpRequest entity class representing the HelpRequest table in the database
 */
public class HelpRequest {
    private int requestId;
    private String title;
    private Date startDate;
    private String emergencyLevel;
    private String description;
    private String personInNeedID;
    private String status;

    public HelpRequest() {
    }

    public HelpRequest(int requestId, String title, Date startDate, String emergencyLevel, String description,
                      String personInNeedID, String status) {
        this.requestId = requestId;
        this.title = title;
        this.startDate = startDate;
        this.emergencyLevel = emergencyLevel;
        this.description = description;
        this.personInNeedID = personInNeedID;
        this.status = status;
    }

    public int getRequestId() {
        return requestId;
    }

    public void setRequestId(int requestId) {
        this.requestId = requestId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Date getStartDate() {
        return startDate;
    }

    public void setStartDate(Date startDate) {
        this.startDate = startDate;
    }

    public String getEmergencyLevel() {
        return emergencyLevel;
    }

    public void setEmergencyLevel(String emergencyLevel) {
        this.emergencyLevel = emergencyLevel;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getPersonInNeedID() {
        return personInNeedID;
    }

    public void setPersonInNeedID(String personInNeedID) {
        this.personInNeedID = personInNeedID;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
