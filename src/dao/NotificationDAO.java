package dao;

import entity.notifications.Notification;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import utils.AppConstants;

/**
 * NotificationDAO class handles all database operations related to notifications.
 */
public class NotificationDAO {

    private static final String DB_URL = "jdbc:sqlite:assets/db/SoiDayGanKet_sqlite.db";

    /**
     * Creates a registration notification for a Volunteer for an event.
     * @param eventId ID of the event
     * @param volunteerUsername Username of the Volunteer
     * @return true if creation is successful, false otherwise.
     */
    public boolean createRegistrationNotification(int eventId, String volunteerUsername) {
        if (isVolunteerPendingOrRegistered(volunteerUsername, eventId)) {
            System.out.println("Volunteer " + volunteerUsername + " already has a pending/registered notification for event " + eventId);
            return false;
        }

        String sql = "INSERT INTO Notification (eventId, username, acceptStatus) VALUES (?, ?, ?)";
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, eventId);
            pstmt.setString(2, volunteerUsername);
            pstmt.setString(3, AppConstants.NOTIF_PENDING);

            int affectedRows = pstmt.executeUpdate();
            return affectedRows > 0;

        } catch (SQLException e) {
            System.err.println("Error creating registration notification: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * Gets the latest notification status for a volunteer for a specific event.
     * @param volunteerUsername The volunteer's username
     * @param eventId The event's ID
     * @return The status string or null if not found.
     */
    public String getVolunteerNotificationStatusForEvent(String volunteerUsername, int eventId) {
        String sql = "SELECT acceptStatus FROM Notification " +
                     "WHERE username = ? AND eventId = ? " +
                     "ORDER BY notificationId DESC LIMIT 1"; 

        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, volunteerUsername);
            pstmt.setInt(2, eventId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("acceptStatus");
                }
            }
        } catch (SQLException e) {
            System.err.println("Error getting volunteer notification status: " + e.getMessage());
        }
        return null;
    }

    /**
     * Checks if a Volunteer already has a 'Pending' or 'Registered' notification for a specific event.
     * @param volunteerUsername The volunteer's username
     * @param eventId The event's ID
     * @return true if such a notification exists, false otherwise.
     */
    public boolean isVolunteerPendingOrRegistered(String volunteerUsername, int eventId) {
        String sql = "SELECT COUNT(*) FROM Notification WHERE username = ? AND eventId = ? AND (acceptStatus = ? OR acceptStatus = ?)";
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, volunteerUsername);
            pstmt.setInt(2, eventId);
            pstmt.setString(3, AppConstants.NOTIF_PENDING);
            pstmt.setString(4, AppConstants.NOTIF_REGISTERED);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        } catch (SQLException e) {
            System.err.println("Error checking existing notification: " + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }

    public List<Notification> getPendingNotificationsByOrganizer(String organizerUsername) {
        List<Notification> result = new ArrayList<>();
        String sql = "SELECT n.notificationId, n.eventId, n.username, n.acceptStatus, e.title " +
                     "FROM Notification n " +
                     "JOIN Events e ON n.eventId = e.eventId " +
                     "WHERE e.organizer = ? AND n.acceptStatus = ?";
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, organizerUsername);
            pstmt.setString(2, AppConstants.NOTIF_PENDING);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    Notification no = new Notification();
                    no.setNotificationId(rs.getInt("notificationId"));
                    no.setEventId(rs.getInt("eventId"));
                    no.setUsername(rs.getString("username"));
                    no.setAcceptStatus(rs.getString("acceptStatus"));
                    no.setEventTitle(rs.getString("title"));
                    result.add(no);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return result;
    }

    /**
     * Processes a registration notification. It updates the notification status and, if the new status
     * is 'Registered', it adds the volunteer to the EventParticipants table within a single transaction.
     * @param notificationId The ID of the notification to process.
     * @param newStatus The new status (e.g., "Registered", "Rejected").
     * @return true if the operation was successful, false otherwise.
     */
    public boolean processRegistrationAndUpdateParticipant(int notificationId, String newStatus) {
        String updateNotificationSql = "UPDATE Notification SET acceptStatus = ? WHERE notificationId = ?";
        String getNotificationDetailsSql = "SELECT eventId, username FROM Notification WHERE notificationId = ?";
        String insertParticipantSql = "INSERT INTO EventParticipants (eventId, username) VALUES (?, ?)";

        Connection conn = null;
        try {
            conn = DriverManager.getConnection(DB_URL);
            conn.setAutoCommit(false);

            // Step 1: Update notification status
            try (PreparedStatement pstmtUpdate = conn.prepareStatement(updateNotificationSql)) {
                pstmtUpdate.setString(1, newStatus);
                pstmtUpdate.setInt(2, notificationId);
                int affectedRows = pstmtUpdate.executeUpdate();
                if (affectedRows == 0) {
                    throw new SQLException("Updating notification failed, no rows affected.");
                }
            }

            // Step 2: If approved ('Registered'), add to EventParticipants
            if (AppConstants.NOTIF_REGISTERED.equals(newStatus)) {
                int eventId = -1;
                String username = null;

                // Get eventId and username from the notification
                try (PreparedStatement pstmtSelect = conn.prepareStatement(getNotificationDetailsSql)) {
                    pstmtSelect.setInt(1, notificationId);
                    try (ResultSet rs = pstmtSelect.executeQuery()) {
                        if (rs.next()) {
                            eventId = rs.getInt("eventId");
                            username = rs.getString("username");
                        } else {
                            throw new SQLException("Could not find notification details for ID: " + notificationId);
                        }
                    }
                }

                // Insert into EventParticipants
                if (eventId != -1 && username != null) {
                    try (PreparedStatement pstmtInsert = conn.prepareStatement(insertParticipantSql)) {
                        pstmtInsert.setInt(1, eventId);
                        pstmtInsert.setString(2, username);
                        pstmtInsert.executeUpdate();
                    }
                }
            }

            conn.commit();
            return true;

        } catch (SQLException e) {
            e.printStackTrace();
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException ex) {
                    ex.printStackTrace();
                }
            }
            return false;
        } finally {
            if (conn != null) {
                try {
                    conn.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * A utility method to close database resources safely.
     * @param conn The database connection
     * @param stmt The statement
     * @param rs The result set
     */
    private void closeResources(Connection conn, Statement stmt, ResultSet rs) {
        try {
            if (rs != null) rs.close();
            if (stmt != null) stmt.close();
            if (conn != null) conn.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
} 