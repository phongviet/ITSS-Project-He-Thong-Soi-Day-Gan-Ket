package controller.event;

import entity.events.Event;
import entity.users.VolunteerOrganization;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class EventController {

    private static final String DB_URL = "jdbc:sqlite:assets/db/SoiDayGanKet_sqlite.db";

    public boolean registerEvent(Event event, VolunteerOrganization organization) {
        Connection conn = null;
        PreparedStatement pstmt = null;

        try {
            conn = DriverManager.getConnection(DB_URL);

            // Start a transaction
            conn.setAutoCommit(false);

            // Set the initial status to "pending"
            event.setStatus("pending");

            // Insert into Events table
            String insertEventSQL = "INSERT INTO Events (title, maxParticipantNumber, supportType, " +
                    "startDay, startMonth, startYear, endDay, endMonth, endYear, " +
                    "emergencyLevel, description, organizer, status) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

            pstmt = conn.prepareStatement(insertEventSQL, Statement.RETURN_GENERATED_KEYS);
            pstmt.setString(1, event.getTitle());
            pstmt.setInt(2, event.getMaxParticipantNumber());
            pstmt.setString(3, event.getSupportType());
            pstmt.setInt(4, event.getStartDay());
            pstmt.setInt(5, event.getStartMonth());
            pstmt.setInt(6, event.getStartYear());
            pstmt.setInt(7, event.getEndDay());
            pstmt.setInt(8, event.getEndMonth());
            pstmt.setInt(9, event.getEndYear());
            pstmt.setString(10, event.getEmergencyLevel());
            pstmt.setString(11, event.getDescription());
            pstmt.setString(12, organization.getUsername());
            pstmt.setString(13, event.getStatus()); // Adding the status field

            int affectedRows = pstmt.executeUpdate();

            if (affectedRows == 0) {
                throw new SQLException("Creating event failed, no rows affected.");
            }

            // Get the generated event ID
            ResultSet generatedKeys = pstmt.getGeneratedKeys();
            if (generatedKeys.next()) {
                int eventId = generatedKeys.getInt(1);
                event.setEventId(eventId);
            } else {
                throw new SQLException("Creating event failed, no ID obtained.");
            }

            // Commit the transaction
            conn.commit();
            return true;

        } catch (SQLException e) {
            // If there is an error, rollback the transaction
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException ex) {
                    ex.printStackTrace();
                }
            }
            e.printStackTrace();
            return false;

        } finally {
            // Close resources
            if (pstmt != null) {
                try {
                    pstmt.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
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
     * Retrieves all events created by a specific organization
     *
     * @param organizerId The ID (username) of the organization
     * @return List of events organized by the specified organization
     */
    public List<Event> getEventsByOrganizerId(String organizerId) {
        List<Event> events = new ArrayList<>();
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;

        try {
            conn = DriverManager.getConnection(DB_URL);
            String sql = "SELECT * FROM Events WHERE organizer = ?";
            pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, organizerId);
            rs = pstmt.executeQuery();

            while (rs.next()) {
                Event event = new Event();
                event.setEventId(rs.getInt("eventId"));
                event.setTitle(rs.getString("title"));
                event.setMaxParticipantNumber(rs.getInt("maxParticipantNumber"));
                event.setSupportType(rs.getString("supportType"));

                // Set dates
                event.setStartDay(rs.getInt("startDay"));
                event.setStartMonth(rs.getInt("startMonth"));
                event.setStartYear(rs.getInt("startYear"));
                event.setEndDay(rs.getInt("endDay"));
                event.setEndMonth(rs.getInt("endMonth"));
                event.setEndYear(rs.getInt("endYear"));

                event.setEmergencyLevel(rs.getString("emergencyLevel"));
                event.setDescription(rs.getString("description"));

                // Get status from database, default to "pending" if null
                String status = rs.getString("status");
                event.setStatus(status != null ? status : "pending");

                events.add(event);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            try {
                if (rs != null) rs.close();
                if (pstmt != null) pstmt.close();
                if (conn != null) conn.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }

        return events;
    }

    // Additional methods for updating events, retrieving events, etc. can be added here
}
