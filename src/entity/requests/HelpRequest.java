package entity.requests;

import java.util.Date; // Re-adding Date import

/**
 * HelpRequest entity class representing the HelpRequest table in the database
 */
public class HelpRequest {
    private int requestId;
    private String title;
    private String description;
    private String contact;
    private String personInNeedUsername;
    private Date startDate;
    private String emergencyLevel;
    private String status;
    private String address;

    public HelpRequest() {
    }

    // Updated constructor
    public HelpRequest(int requestId, String title, String description, String contact,
                       String personInNeedUsername, Date startDate, String emergencyLevel, String status) {
        this.requestId = requestId;
        this.title = title;
        this.description = description;
        this.contact = contact;
        this.personInNeedUsername = personInNeedUsername;
        this.startDate = startDate;
        this.emergencyLevel = emergencyLevel;
        this.status = status;
    }

    public int getRequestId() { // Changed from getId
        return requestId;
    }

    public void setRequestId(int requestId) { // Changed from setId
        this.requestId = requestId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getContact() {
        return contact;
    }

    public void setContact(String contact) {
        this.contact = contact;
    }

    public String getPersonInNeedUsername() {
        return personInNeedUsername;
    }

    public void setPersonInNeedUsername(String personInNeedUsername) {
        this.personInNeedUsername = personInNeedUsername;
    }

    public Date getStartDate() { // Added back
        return startDate;
    }

    public void setStartDate(Date startDate) { // Added back
        this.startDate = startDate;
    }

    public String getEmergencyLevel() { // Added back
        return emergencyLevel;
    }

    public void setEmergencyLevel(String emergencyLevel) { // Added back
        this.emergencyLevel = emergencyLevel;
    }

    public String getStatus() { // Added back (replaces isFulfilled getter)
        return status;
    }

    public void setStatus(String status) { // Added back (replaces isFulfilled setter)
        this.status = status;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }
}
