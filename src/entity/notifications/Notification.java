package entity.notifications;

/**
 * Notification entity class representing the Notification table in the database
 */
public class Notification {
    private int notificationId;
    private Integer eventId;
    private String username;
    private String acceptStatus;
    private String eventTitle;

    public Notification() {
    }

    public Notification(int notificationId, Integer eventId, String username, String acceptStatus) {
        this.notificationId = notificationId;
        this.eventId = eventId;
        this.username = username;
        this.acceptStatus = acceptStatus;
    }

    public int getNotificationId() {
        return notificationId;
    }

    public void setNotificationId(int notificationId) {
        this.notificationId = notificationId;
    }

    public Integer getEventId() {
        return eventId;
    }

    public void setEventId(Integer eventId) {
        this.eventId = eventId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getAcceptStatus() {
        return acceptStatus;
    }

    public void setAcceptStatus(String acceptStatus) {
        this.acceptStatus = acceptStatus;
    }

    public String getEventTitle() {
        return eventTitle;
    }

    public void setEventTitle(String eventTitle) {
        this.eventTitle = eventTitle;
    }
}
