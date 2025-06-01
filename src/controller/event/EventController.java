package controller.event;

import entity.events.Event;
import entity.users.VolunteerOrganization;
import entity.users.Volunteer;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Date;
import java.util.Calendar;
import java.text.SimpleDateFormat;

public class EventController {

    private static final String DB_URL = "jdbc:sqlite:assets/db/SoiDayGanKet_sqlite.db";
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");

    public boolean registerEvent(Event event, VolunteerOrganization organization) {
        Connection conn = null;
        PreparedStatement pstmt = null;

        try {
            conn = DriverManager.getConnection(DB_URL);

            // Start a transaction
            conn.setAutoCommit(false);

            // Set the initial status to "pending"
            event.setStatus("pending");

            // Format dates for SQLite
            String startDateStr = null;
            String endDateStr = null;

            if (event.getStartDate() != null) {
                startDateStr = DATE_FORMAT.format(event.getStartDate());
            }

            if (event.getEndDate() != null) {
                endDateStr = DATE_FORMAT.format(event.getEndDate());
            }

            // Insert into Events table using startDate and endDate columns
            String insertEventSQL = "INSERT INTO Events (title, maxParticipantNumber, startDate, endDate, " +
                    "emergencyLevel, description, organizer, status) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

            pstmt = conn.prepareStatement(insertEventSQL, Statement.RETURN_GENERATED_KEYS);
            pstmt.setString(1, event.getTitle());
            pstmt.setInt(2, event.getMaxParticipantNumber());
            pstmt.setString(3, startDateStr);
            pstmt.setString(4, endDateStr);
            pstmt.setString(5, event.getEmergencyLevel());
            pstmt.setString(6, event.getDescription());
            pstmt.setString(7, organization.getUsername());
            pstmt.setString(8, event.getStatus());

            int affectedRows = pstmt.executeUpdate();

            if (affectedRows == 0) {
                throw new SQLException("Creating event failed, no rows affected.");
            }

            // Get the generated event ID
            ResultSet generatedKeys = pstmt.getGeneratedKeys();
            if (generatedKeys.next()) {
                int eventId = generatedKeys.getInt(1);
                event.setEventId(eventId);

                // Insert required skills
                if (event.getRequiredSkills() != null && !event.getRequiredSkills().isEmpty()) {
                    // Lấy ID của kỹ năng từ bảng Skills
                    for (String skillName : event.getRequiredSkills()) {
                        int skillId = getSkillIdByName(conn, skillName);
                        if (skillId > 0) {
                            insertEventSkill(conn, eventId, skillId);
                        }
                    }
                }
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
     * Phương thức trợ giúp để lấy skillId dựa vào tên kỹ năng
     */
    private int getSkillIdByName(Connection conn, String skillName) throws SQLException {
        String sql = "SELECT skillId FROM Skills WHERE skill = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, skillName);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("skillId");
                }
            }
        }
        return -1; // Không tìm thấy
    }

    /**
     * Phương thức trợ giúp để thêm kỹ năng cho event
     */
    private void insertEventSkill(Connection conn, int eventId, int skillId) throws SQLException {
        String sql = "INSERT INTO EventSkills (eventId, skillId) VALUES (?, ?)";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, eventId);
            pstmt.setInt(2, skillId);
            pstmt.executeUpdate();
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
                int eventId = rs.getInt("eventId");
                event.setEventId(eventId);
                event.setTitle(rs.getString("title"));
                event.setMaxParticipantNumber(rs.getInt("maxParticipantNumber"));

                // Lấy ngày từ cột DATE trong DB
                try {
                    String startDateStr = rs.getString("startDate");
                    String endDateStr = rs.getString("endDate");

                    if (startDateStr != null && !startDateStr.isEmpty()) {
                        try {
                            // Parse từ chuỗi định dạng YYYY-MM-DD
                            event.setStartDate(DATE_FORMAT.parse(startDateStr));
                        } catch (Exception ex) {
                            System.err.println("Không thể chuyển đổi ngày bắt đầu: " + startDateStr);
                        }
                    }

                    if (endDateStr != null && !endDateStr.isEmpty()) {
                        try {
                            // Parse từ chuỗi định dạng YYYY-MM-DD
                            event.setEndDate(DATE_FORMAT.parse(endDateStr));
                        } catch (Exception ex) {
                            System.err.println("Không thể chuyển đổi ngày kết thúc: " + endDateStr);
                        }
                    }
                } catch (SQLException e) {
                    System.err.println("Lỗi khi đọc ngày tháng: " + e.getMessage());
                }

                event.setEmergencyLevel(rs.getString("emergencyLevel"));
                event.setDescription(rs.getString("description"));
                event.setOrganizer(rs.getString("organizer"));
                event.setNeeder(rs.getString("needer"));

                // Get status from database, default to "pending" if null
                String status = rs.getString("status");
                event.setStatus(status != null ? status : "pending");

                // Load required skills for this event
                loadEventSkills(conn, event);

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

    /**
     * Load the required skills for a given event
     *
     * @param conn Database connection
     * @param event The event to load skills for
     */
    private void loadEventSkills(Connection conn, Event event) throws SQLException {
        String sql = "SELECT s.skill FROM EventSkills es JOIN Skills s ON es.skillId = s.skillId WHERE es.eventId = ?";
        PreparedStatement pstmt = conn.prepareStatement(sql);
        pstmt.setInt(1, event.getEventId());
        ResultSet rs = pstmt.executeQuery();

        while (rs.next()) {
            event.addRequiredSkill(rs.getString("skill"));
        }

        rs.close();
        pstmt.close();
    }

    /**
     * Get the list of volunteers participating in an event
     *
     * @param eventId The ID of the event
     * @return List of volunteers participating in the event
     */
    public List<Volunteer> getEventParticipants(int eventId) {
        List<Volunteer> participants = new ArrayList<>();
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;

        try {
            conn = DriverManager.getConnection(DB_URL);
            // Join EventParticipants with Volunteers to get volunteer details
            String sql = "SELECT v.* FROM EventParticipants ep " +
                         "JOIN Volunteer v ON ep.username = v.username " +
                         "WHERE ep.eventId = ?";
            pstmt = conn.prepareStatement(sql);
            pstmt.setInt(1, eventId);
            rs = pstmt.executeQuery();

            while (rs.next()) {
                Volunteer volunteer = new Volunteer();
                volunteer.setUsername(rs.getString("username"));
                volunteer.setFullName(rs.getString("fullName"));
                // Set other properties as needed

                participants.add(volunteer);
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

        return participants;
    }

    /**
     * Register a volunteer to participate in an event
     *
     * @param eventId The ID of the event
     * @param volunteerId The ID (username) of the volunteer
     * @return true if registration was successful, false otherwise
     */
    public boolean registerVolunteerForEvent(int eventId, String volunteerId) {
        Connection conn = null;
        PreparedStatement pstmt = null;

        try {
            conn = DriverManager.getConnection(DB_URL);

            // Check if the event is approved and not full
            String checkEventSQL = "SELECT status, maxParticipantNumber, " +
                                  "(SELECT COUNT(*) FROM EventParticipants WHERE eventId = ?) as currentParticipants " +
                                  "FROM Events WHERE eventId = ?";
            pstmt = conn.prepareStatement(checkEventSQL);
            pstmt.setInt(1, eventId);
            pstmt.setInt(2, eventId);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                String status = rs.getString("status");
                int maxParticipants = rs.getInt("maxParticipantNumber");
                int currentParticipants = rs.getInt("currentParticipants");

                if (!"approved".equals(status)) {
                    return false; // Event is not approved
                }

                if (currentParticipants >= maxParticipants) {
                    return false; // Event is full
                }
            } else {
                return false; // Event not found
            }

            rs.close();
            pstmt.close();

            // Insert the volunteer registration
            String insertSQL = "INSERT INTO EventParticipants (eventId, username) VALUES (?, ?)";
            pstmt = conn.prepareStatement(insertSQL);
            pstmt.setInt(1, eventId);
            pstmt.setString(2, volunteerId);

            int affectedRows = pstmt.executeUpdate();
            return affectedRows > 0;

        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        } finally {
            try {
                if (pstmt != null) pstmt.close();
                if (conn != null) conn.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Get all available events with their current participation status
     *
     * @return List of events with participation information
     */
    public List<Event> getAllEvents() {
        List<Event> events = new ArrayList<>();
        Connection conn = null;
        Statement stmt = null;
        ResultSet rs = null;

        try {
            conn = DriverManager.getConnection(DB_URL);
            stmt = conn.createStatement();
            String sql = "SELECT e.*, " +
                        "(SELECT COUNT(*) FROM EventParticipants WHERE eventId = e.eventId) as participantCount " +
                        "FROM Events e";
            rs = stmt.executeQuery(sql);

            while (rs.next()) {
                Event event = new Event();
                int eventId = rs.getInt("eventId");
                event.setEventId(eventId);
                event.setTitle(rs.getString("title"));
                event.setMaxParticipantNumber(rs.getInt("maxParticipantNumber"));

                // Lấy ngày từ cột DATE trong DB
                try {
                    String startDateStr = rs.getString("startDate");
                    String endDateStr = rs.getString("endDate");

                    if (startDateStr != null && !startDateStr.isEmpty()) {
                        try {
                            // Parse từ chuỗi định dạng YYYY-MM-DD
                            event.setStartDate(DATE_FORMAT.parse(startDateStr));
                        } catch (Exception ex) {
                            System.err.println("Không thể chuyển đổi ngày bắt đầu: " + startDateStr);
                        }
                    }

                    if (endDateStr != null && !endDateStr.isEmpty()) {
                        try {
                            // Parse từ chuỗi định dạng YYYY-MM-DD
                            event.setEndDate(DATE_FORMAT.parse(endDateStr));
                        } catch (Exception ex) {
                            System.err.println("Không thể chuyển đổi ngày kết thúc: " + endDateStr);
                        }
                    }
                } catch (SQLException e) {
                    System.err.println("Lỗi khi đọc ngày tháng: " + e.getMessage());
                }

                event.setEmergencyLevel(rs.getString("emergencyLevel"));
                event.setDescription(rs.getString("description"));
                event.setOrganizer(rs.getString("organizer"));
                event.setNeeder(rs.getString("needer"));
                event.setStatus(rs.getString("status"));

                // Load required skills for this event
                loadEventSkills(conn, event);

                events.add(event);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return events;
    }
}
