package controller.notification;

import dao.NotificationDAO;
import java.util.List;

public class NotificationController {
    private NotificationDAO notificationDAO;

    public NotificationController() {
        this.notificationDAO = new NotificationDAO();
    }

    /**
     * Creates a registration notification for a Volunteer to join an event.
     * @param eventId The ID of the event
     * @param volunteerUsername The username of the Volunteer
     * @return true if the notification was created successfully, false otherwise.
     */
    public boolean createRegistrationNotification(int eventId, String volunteerUsername) {
        return notificationDAO.createRegistrationNotification(eventId, volunteerUsername);
    }
    
    /**
     * Gets the latest notification status of a Volunteer for an Event.
     * @param volunteerUsername The volunteer's username.
     * @param eventId The event's ID.
     * @return The status string (e.g., "Pending", "Registered") or null if no notification is found.
     */
    public String getVolunteerNotificationStatusForEvent(String volunteerUsername, int eventId) {
        return notificationDAO.getVolunteerNotificationStatusForEvent(volunteerUsername, eventId);
    }

    /**
     * Checks if a Volunteer has a pending or registered notification for a specific event.
     * @param volunteerUsername The volunteer's username.
     * @param eventId The event's ID.
     * @return true if a relevant notification exists, false otherwise.
     */
    public boolean isVolunteerPendingOrRegistered(String volunteerUsername, int eventId) {
        return notificationDAO.isVolunteerPendingOrRegistered(volunteerUsername, eventId);
    }

    public List<entity.notifications.Notification> getPendingNotificationsByOrganizer(String organizerUsername) {
        return notificationDAO.getPendingNotificationsByOrganizer(organizerUsername);
    }

    public boolean updateNotificationStatus(int notificationId, String newStatus) {
        return notificationDAO.processRegistrationAndUpdateParticipant(notificationId, newStatus);
    }
}