package entity.events;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class Event {
    private int eventId;
    private String title;
    private int maxParticipantNumber;
    private Date startDate;
    private Date endDate;
    private String emergencyLevel;
    private String description;
    private ArrayList<String> requiredSkills; // Skills as ArrayList
    private String organizer;
    private String status;   // "pending", "approved", etc.

    public Event() {
        this.requiredSkills = new ArrayList<>();
    }

    public Event(int eventId, String title, int maxParticipantNumber, Date startDate, Date endDate,
                String emergencyLevel, String description, String organizer, String status) {
        this.eventId = eventId;
        this.title = title;
        this.maxParticipantNumber = maxParticipantNumber;
        this.startDate = startDate;
        this.endDate = endDate;
        this.emergencyLevel = emergencyLevel;
        this.description = description;
        this.organizer = organizer;
        this.status = status;
        this.requiredSkills = new ArrayList<>();
    }

    public int getEventId() {
        return eventId;
    }

    public void setEventId(int eventId) {
        this.eventId = eventId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public int getMaxParticipantNumber() {
        return maxParticipantNumber;
    }

    public void setMaxParticipantNumber(int maxParticipantNumber) {
        this.maxParticipantNumber = maxParticipantNumber;
    }

    public Date getStartDate() {
        return startDate;
    }

    public void setStartDate(Date startDate) {
        this.startDate = startDate;
    }

    public Date getEndDate() {
        return endDate;
    }

    public void setEndDate(Date endDate) {
        this.endDate = endDate;
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

    public ArrayList<String> getRequiredSkills() {
        return requiredSkills;
    }

    public void setRequiredSkills(ArrayList<String> requiredSkills) {
        this.requiredSkills = requiredSkills;
    }

    public void addRequiredSkill(String skill) {
        if (this.requiredSkills == null) {
            this.requiredSkills = new ArrayList<>();
        }
        this.requiredSkills.add(skill);
    }

    public String getOrganizer() {
        return organizer;
    }

    public void setOrganizer(String organizer) {
        this.organizer = organizer;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getNeeder() {
        return null; // No longer used
    }

    public void setNeeder(String needer) {
        // No longer used
    }
}
