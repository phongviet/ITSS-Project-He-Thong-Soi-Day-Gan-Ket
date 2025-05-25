package controller.event;

import entity.events.Event;
import entity.users.VolunteerOrganization;
import java.sql.*;

public class EventController {

    private static final String DB_URL = "jdbc:sqlite:assets/db/SoiDayGanKet_sqlite.db";

    public boolean registerEvent(Event event, VolunteerOrganization organization) {
        Connection conn = null;
        PreparedStatement pstmt = null;

        try {
            conn = DriverManager.getConnection(DB_URL);

            // Start a transaction
            conn.setAutoCommit(false);

            // Insert into Events table
            String insertEventSQL = "INSERT INTO Events (title, maxParticipantNumber, supportType, " +
                    "startDay, startMonth, startYear, endDay, endMonth, endYear, " +
                    "emergencyLevel, description, organizer) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

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

    // Additional methods for updating events, retrieving events, etc. can be added here
}