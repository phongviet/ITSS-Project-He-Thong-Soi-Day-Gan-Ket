package controller;

import entity.events.Event;
import entity.requests.HelpRequest;
import entity.users.Admin;
import java.sql.*;
import java.util.List;
import java.util.ArrayList;
import java.text.SimpleDateFormat;

/**
 * Controller class for handling administrative approval of help requests and events
 */
public class AdminApprovalController {

    private Admin admin;
    private static final String DB_URL = "jdbc:sqlite:assets/db/SoiDayGanKet_sqlite.db";

    /**
     * Constructor for the AdminApprovalController
     *
     */
    public AdminApprovalController() {
    }

    /**
     * Retrieves all pending help requests that need admin approval
     *
     * @return List of pending help requests
     */
    public List<HelpRequest> getPendingHelpRequests() {
        // This will be implemented later
        return null;
    }

    /**
     * Approves a help request
     *
     * @param helpRequest The help request to approve
     * @return true if approval was successful, false otherwise
     */
    public boolean approveHelpRequest(HelpRequest helpRequest) {
        // This will be implemented later
        return false;
    }

    /**
     * Rejects a help request
     *
     * @param helpRequest The help request to reject
     * @param reason The reason for rejection
     * @return true if rejection was successful, false otherwise
     */
    public boolean rejectHelpRequest(HelpRequest helpRequest, String reason) {
        // This will be implemented later
        return false;
    }

    /**
     * Retrieves all pending events that need admin approval
     *
     * @return List of pending events
     */
    public List<Event> getPendingEvents() {
        // This will be implemented later
        return null;
    }

    /**
     * Retrieves all events for admin review
     *
     * @return List of all events in the system
     */
    public List<Event> getAllEvents() {
        // This will be implemented later
        return null;
    }

    /**
     * Approves an event
     *
     * @param eventId The ID of the event to approve
     * @return true if approval was successful, false otherwise
     */
    public boolean approveEvent(int eventId) {
        try (Connection connection = DriverManager.getConnection(DB_URL)) {
            // Get the event by ID
            Event event = findEventById(eventId);
            if (event == null) {
                return false;
            }

            // Update event status to Coming Soon in the database
            String updateQuery = "UPDATE Events SET status = ? WHERE eventId = ?";
            try (PreparedStatement preparedStatement = connection.prepareStatement(updateQuery)) {
                preparedStatement.setString(1, "Coming Soon");
                preparedStatement.setInt(2, eventId);
                int rowsUpdated = preparedStatement.executeUpdate();

                // Check if the update was successful
                return rowsUpdated > 0;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Rejects an event
     *
     * @param eventId The ID of the event to reject
     * @return true if rejection was successful, false otherwise
     */
    public boolean rejectEvent(int eventId) {
        try (Connection connection = DriverManager.getConnection(DB_URL)) {
            // Get the event by ID
            Event event = findEventById(eventId);
            if (event == null) {
                return false;
            }

            // Update event status to Rejected in the database
            String updateQuery = "UPDATE Events SET status = ? WHERE eventId = ?";
            try (PreparedStatement preparedStatement = connection.prepareStatement(updateQuery)) {
                preparedStatement.setString(1, "Rejected");
                preparedStatement.setInt(2, eventId);
                int rowsUpdated = preparedStatement.executeUpdate();

                // Check if the update was successful
                return rowsUpdated > 0;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Helper method to find an event by its ID
     *
     * @param eventId The ID of the event to find
     * @return The Event object if found, null otherwise
     */
    private Event findEventById(int eventId) {
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            conn = DriverManager.getConnection(DB_URL);
            String sql = "SELECT * FROM Events WHERE eventId = ?";
            stmt = conn.prepareStatement(sql);
            stmt.setInt(1, eventId);
            rs = stmt.executeQuery();

            if (rs.next()) {
                Event event = new Event();
                event.setEventId(rs.getInt("eventId"));
                event.setTitle(rs.getString("title"));
                event.setMaxParticipantNumber(rs.getInt("maxParticipantNumber"));

                // Handle date conversion
                String startDateStr = rs.getString("startDate");
                String endDateStr = rs.getString("endDate");

                if (startDateStr != null && !startDateStr.isEmpty()) {
                    try {
                        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
                        event.setStartDate(dateFormat.parse(startDateStr));
                    } catch (Exception e) {
                        System.out.println("Error parsing start date: " + e.getMessage());
                    }
                }

                if (endDateStr != null && !endDateStr.isEmpty()) {
                    try {
                        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
                        event.setEndDate(dateFormat.parse(endDateStr));
                    } catch (Exception e) {
                        System.out.println("Error parsing end date: " + e.getMessage());
                    }
                }

                event.setEmergencyLevel(rs.getString("emergencyLevel"));
                event.setDescription(rs.getString("description"));
                event.setOrganizer(rs.getString("organizer"));
                event.setStatus(rs.getString("status"));

                return event;
            }
            return null;
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        } finally {
            closeResources(conn, stmt, rs);
        }
    }

    /**
     * Close database connection and statement resources
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
