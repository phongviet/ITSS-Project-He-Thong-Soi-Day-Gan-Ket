package controller;

import entity.events.Event;
import entity.requests.HelpRequest;
import entity.users.Admin;
import java.sql.*;
import java.util.List;
import java.util.ArrayList;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Controller class for handling administrative approval of help requests and events
 */
public class AdminApprovalController {

    // Database URL for SQLite
    private static final String DB_URL = "jdbc:sqlite:assets/db/SoiDayGanKet_sqlite.db";

    /**
     * Constructor for the AdminApprovalController
     *
     */
    public AdminApprovalController() {
    }

    /**
     * Approves an event
     *
     * @param eventId The ID of the event to approve
     * @return true if approval was successful, false otherwise
     */
    public boolean approveEvent(int eventId) {
        Connection connection = null;
        try {
            connection = DriverManager.getConnection(DB_URL);
            connection.setAutoCommit(false);

            // Get the event by ID
            Event event = findEventById(eventId);
            if (event == null) {
                return false;
            }

            // Update event status to Upcoming in the database
            String updateEventQuery = "UPDATE Events SET status = ? WHERE eventId = ?";
            try (PreparedStatement preparedStatement = connection.prepareStatement(updateEventQuery)) {
                preparedStatement.setString(1, "Upcoming");
                preparedStatement.setInt(2, eventId);
                int rowsUpdated = preparedStatement.executeUpdate();

                if (rowsUpdated <= 0) {
                    connection.rollback();
                    return false;
                }
            }

            // Check if this event is associated with a help request
            String checkRequestQuery = "SELECT requestId FROM Events WHERE eventId = ? AND requestId IS NOT NULL";
            try (PreparedStatement checkStmt = connection.prepareStatement(checkRequestQuery)) {
                checkStmt.setInt(1, eventId);
                ResultSet rs = checkStmt.executeQuery();

                // If associated request exists, update its status to "Closed"
                if (rs.next()) {
                    String requestId = rs.getString("requestId");
                    if (requestId != null && !requestId.isEmpty()) {
                        String updateRequestQuery = "UPDATE HelpRequest SET status = 'Closed' WHERE requestId = ?";
                        try (PreparedStatement requestStmt = connection.prepareStatement(updateRequestQuery)) {
                            requestStmt.setString(1, requestId);
                            requestStmt.executeUpdate();
                        }
                    }
                }
            }

            connection.commit();
            return true;
        } catch (SQLException e) {
            try {
                if (connection != null) {
                    connection.rollback();
                }
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
            e.printStackTrace();
            return false;
        } finally {
            try {
                if (connection != null) {
                    connection.setAutoCommit(true);
                    connection.close();
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
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
     * Retrieves all help requests for admin review
     *
     * @return List of all help requests in the system
     */
    public List<HelpRequest> getAllHelpRequests() {
        List<HelpRequest> helpRequests = new ArrayList<>();

        try (Connection connection = DriverManager.getConnection(DB_URL);
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery("SELECT * FROM HelpRequest")) {

            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
            while (resultSet.next()) {
                HelpRequest helpRequest = new HelpRequest();

                helpRequest.setRequestId(resultSet.getInt("requestId"));
                helpRequest.setTitle(resultSet.getString("title"));

                String startDateStr = resultSet.getString("startDate");
                if (startDateStr != null && !startDateStr.isEmpty()) {
                    try {
                        helpRequest.setStartDate(dateFormat.parse(startDateStr));
                    } catch (Exception e) {
                        System.out.println("Error parsing start date: " + e.getMessage());
                    }
                }

                helpRequest.setEmergencyLevel(resultSet.getString("emergencyLevel"));
                helpRequest.setDescription(resultSet.getString("description"));
                helpRequest.setPersonInNeedId(resultSet.getString("personInNeedId"));
                helpRequest.setStatus(resultSet.getString("status"));

                helpRequests.add(helpRequest);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return helpRequests;
    }

    /**
     * Approves a help request
     *
     * @param helpRequest The help request to approve
     * @return true if approval was successful, false otherwise
     */
    public boolean approveHelpRequest(HelpRequest helpRequest) {
        try (Connection connection = DriverManager.getConnection(DB_URL)) {
            String updateQuery = "UPDATE HelpRequest SET status = ? WHERE requestId = ?";
            try (PreparedStatement preparedStatement = connection.prepareStatement(updateQuery)) {
                preparedStatement.setString(1, "Approved");
                preparedStatement.setInt(2, helpRequest.getRequestId());
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
     * Rejects a help request
     *
     * @param helpRequest The help request to reject
     * @return true if rejection was successful, false otherwise
     */
    public boolean rejectHelpRequest(HelpRequest helpRequest) {
        try (Connection connection = DriverManager.getConnection(DB_URL)) {
            String updateQuery = "UPDATE HelpRequest SET status = ? WHERE requestId = ?";
            try (PreparedStatement preparedStatement = connection.prepareStatement(updateQuery)) {
                preparedStatement.setString(1, "Rejected");
                preparedStatement.setInt(2, helpRequest.getRequestId());
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

