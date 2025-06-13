package dao;

import entity.events.Event;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.text.SimpleDateFormat;
import java.text.ParseException;
import utils.AppConstants;
import entity.events.EventParticipantDetails;
import entity.users.VolunteerOrganization;
import java.util.ArrayList;

/**
 * EventDAO class handles all database operations related to events.
 * This includes creating, reading, updating, and deleting events,
 * as well as managing participants and required skills.
 */
public class EventDAO {

    private static final String DB_URL = "jdbc:sqlite:assets/db/SoiDayGanKet_sqlite.db";

    // Methods for event management will be moved here from EventController.
    // For example:
    // public Event getEventById(int eventId) { ... }
    // public List<Event> getAllEvents() { ... }
    // public boolean saveEvent(Event event) { ... }
    // public boolean addParticipantToEvent(int eventId, String username) { ... }

    public Map<String, Object> getEventStatistics() {
        Map<String, Object> stats = new HashMap<>();
        
        String totalEventsQuery = "SELECT COUNT(*) FROM Events";
        String upcomingEventsQuery = "SELECT COUNT(*) FROM Events WHERE startDate > date('now')";
        String ongoingEventsQuery = "SELECT COUNT(*) FROM Events WHERE startDate <= date('now') AND endDate >= date('now')";
        String pastEventsQuery = "SELECT COUNT(*) FROM Events WHERE endDate < date('now')";

        try (Connection conn = DriverManager.getConnection(DB_URL);
             Statement stmt = conn.createStatement()) {

            try (ResultSet rs = stmt.executeQuery(totalEventsQuery)) {
                if (rs.next()) {
                    stats.put("totalEvents", rs.getInt(1));
                }
            }
            try (ResultSet rs = stmt.executeQuery(upcomingEventsQuery)) {
                if (rs.next()) {
                    stats.put("upcomingEvents", rs.getInt(1));
                }
            }
            try (ResultSet rs = stmt.executeQuery(ongoingEventsQuery)) {
                if (rs.next()) {
                    stats.put("ongoingEvents", rs.getInt(1));
                }
            }
            try (ResultSet rs = stmt.executeQuery(pastEventsQuery)) {
                if (rs.next()) {
                    stats.put("pastEvents", rs.getInt(1));
                }
            }
            
            // Monthly distribution (example for last 6 months of 2025)
            Map<String, Integer> monthlyEvents = new HashMap<>();
            String[] months = {"Jan", "Feb", "Mar", "Apr", "May", "Jun"};
            String[] dateFilters = {
                "startDate BETWEEN '2025-01-01' AND '2025-01-31'",
                "startDate BETWEEN '2025-02-01' AND '2025-02-28'",
                "startDate BETWEEN '2025-03-01' AND '2025-03-31'",
                "startDate BETWEEN '2025-04-01' AND '2025-04-30'",
                "startDate BETWEEN '2025-05-01' AND '2025-05-31'",
                "startDate BETWEEN '2025-06-01' AND '2025-06-30'"
            };

            for (int i = 0; i < months.length; i++) {
                String monthlyQuery = "SELECT COUNT(*) FROM Events WHERE " + dateFilters[i];
                try(PreparedStatement pstmt = conn.prepareStatement(monthlyQuery);
                    ResultSet rs = pstmt.executeQuery()) {
                    if (rs.next()) {
                        monthlyEvents.put(months[i], rs.getInt(1));
                    }
                }
            }
            stats.put("monthlyEvents", monthlyEvents);

        } catch (SQLException e) {
            e.printStackTrace();
            System.err.println("Error fetching event statistics: " + e.getMessage());
        }
        return stats;
    }

    public Event getEventById(int eventId) {
        String sql = "SELECT * FROM Events WHERE eventId = ?";
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, eventId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    Event event = new Event();
                    event.setEventId(rs.getInt("eventId"));
                    event.setTitle(rs.getString("title"));
                    event.setMaxParticipantNumber(rs.getInt("maxParticipantNumber"));
                    
                    try {
                        String startDateStr = rs.getString("startDate");
                        if (startDateStr != null) {
                            event.setStartDate(new SimpleDateFormat("yyyy-MM-dd").parse(startDateStr));
                        }
                        String endDateStr = rs.getString("endDate");
                        if (endDateStr != null) {
                            event.setEndDate(new SimpleDateFormat("yyyy-MM-dd").parse(endDateStr));
                        }
                    } catch (ParseException e) {
                        System.err.println("Error parsing date for event " + eventId);
                        e.printStackTrace();
                    }
                    
                    event.setEmergencyLevel(rs.getString("emergencyLevel"));
                    event.setDescription(rs.getString("description"));
                    event.setOrganizer(rs.getString("organizer"));
                    event.setRequestId(rs.getString("requestId"));
                    event.setStatus(rs.getString("status"));
                    return event;
                }
            }
        } catch (SQLException e) {
            System.err.println("Error getting event by ID: " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }

    public boolean updateEventStatus(int eventId, String status) {
        String sql = "UPDATE Events SET status = ? WHERE eventId = ?";
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, status);
            pstmt.setInt(2, eventId);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error updating event status: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    public boolean approveEvent(int eventId) {
        Connection conn = null;
        try {
            conn = DriverManager.getConnection(DB_URL);
            conn.setAutoCommit(false);

            // Update event status
            if (!updateEventStatusInTransaction(conn, eventId, AppConstants.EVENT_UPCOMING)) {
                conn.rollback();
                return false;
            }

            // Check and update associated help request
            Event event = getEventById(eventId); // We need request ID from the event
            if (event != null && event.getRequestId() != null && !event.getRequestId().isEmpty()) {
                HelpRequestDAO helpRequestDAO = new HelpRequestDAO();
                if (!helpRequestDAO.updateHelpRequestStatusInTransaction(conn, Integer.parseInt(event.getRequestId()), AppConstants.REQUEST_CLOSED)) {
                    conn.rollback();
                    return false;
                }
            }
            
            conn.commit();
            return true;
        } catch (SQLException e) {
            System.err.println("Error approving event: " + e.getMessage());
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
                    conn.setAutoCommit(true);
                    conn.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
    }
    
    public boolean rejectEvent(int eventId) {
        return updateEventStatus(eventId, AppConstants.EVENT_REJECTED);
    }
    
    boolean updateEventStatusInTransaction(Connection conn, int eventId, String status) throws SQLException {
        String sql = "UPDATE Events SET status = ? WHERE eventId = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, status);
            pstmt.setInt(2, eventId);
            return pstmt.executeUpdate() > 0;
        }
    }

    public boolean registerEvent(Event event, VolunteerOrganization organization) {
        String insertEventSQL = "INSERT INTO Events (title, maxParticipantNumber, startDate, endDate, emergencyLevel, description, organizer, status, requestId) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        Connection conn = null;
        try {
            conn = DriverManager.getConnection(DB_URL);
            conn.setAutoCommit(false);

            event.setStatus(AppConstants.EVENT_PENDING);

            try(PreparedStatement pstmt = conn.prepareStatement(insertEventSQL, Statement.RETURN_GENERATED_KEYS)) {
                pstmt.setString(1, event.getTitle());
                pstmt.setInt(2, event.getMaxParticipantNumber());
                pstmt.setString(3, event.getStartDate() != null ? new SimpleDateFormat("yyyy-MM-dd").format(event.getStartDate()) : null);
                pstmt.setString(4, event.getEndDate() != null ? new SimpleDateFormat("yyyy-MM-dd").format(event.getEndDate()) : null);
                pstmt.setString(5, event.getEmergencyLevel());
                pstmt.setString(6, event.getDescription());
                pstmt.setString(7, organization.getUsername());
                pstmt.setString(8, event.getStatus());
                pstmt.setString(9, event.getRequestId());

                int affectedRows = pstmt.executeUpdate();
                if (affectedRows == 0) throw new SQLException("Creating event failed, no rows affected.");
                
                try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        int eventId = generatedKeys.getInt(1);
                        event.setEventId(eventId);
                        if (event.getRequiredSkills() != null && !event.getRequiredSkills().isEmpty()) {
                            for (String skillName : event.getRequiredSkills()) {
                                int skillId = getSkillIdByName(conn, skillName);
                                if (skillId > 0) insertEventSkill(conn, eventId, skillId);
                            }
                        }
                    } else {
                        throw new SQLException("Creating event failed, no ID obtained.");
                    }
                }
            }
            conn.commit();
            return true;
        } catch (SQLException e) {
            if (conn != null) { try { conn.rollback(); } catch (SQLException ex) { ex.printStackTrace(); } }
            e.printStackTrace();
            return false;
        } finally {
            if (conn != null) { try { conn.close(); } catch (SQLException e) { e.printStackTrace(); } }
        }
    }

    private int getSkillIdByName(Connection conn, String skillName) throws SQLException {
        String sql = "SELECT skillId FROM Skills WHERE skill = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, skillName);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) return rs.getInt("skillId");
            }
        }
        return -1;
    }

    private void insertEventSkill(Connection conn, int eventId, int skillId) throws SQLException {
        String sql = "INSERT INTO EventSkills (eventId, skillId) VALUES (?, ?)";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, eventId);
            pstmt.setInt(2, skillId);
            pstmt.executeUpdate();
        }
    }

    public List<Event> getEventsByOrganizerId(String organizerId) {
        List<Event> events = new ArrayList<>();
        String sql = "SELECT * FROM Events WHERE organizer = ?";
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, organizerId);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                Event event = new Event();
                int eventId = rs.getInt("eventId");
                event.setEventId(eventId);
                event.setTitle(rs.getString("title"));
                event.setMaxParticipantNumber(rs.getInt("maxParticipantNumber"));
                 try {
                    String startDateStr = rs.getString("startDate");
                    if(startDateStr != null) event.setStartDate(new SimpleDateFormat("yyyy-MM-dd").parse(startDateStr));
                    String endDateStr = rs.getString("endDate");
                    if(endDateStr != null) event.setEndDate(new SimpleDateFormat("yyyy-MM-dd").parse(endDateStr));
                } catch (ParseException e) { e.printStackTrace(); }
                event.setEmergencyLevel(rs.getString("emergencyLevel"));
                event.setDescription(rs.getString("description"));
                event.setOrganizer(rs.getString("organizer"));
                event.setRequestId(rs.getString("RequestId"));
                event.setStatus(rs.getString("status"));
                loadEventSkills(conn, event);
                events.add(event);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return events;
    }

    public List<Event> getAllOpenAndAvailableEvents() {
        List<Event> events = new ArrayList<>();
        String sql = "SELECT *, (SELECT COUNT(*) FROM EventParticipants ep WHERE ep.eventId = e.eventId) as currentParticipants FROM Events e WHERE status = ? OR status = ?";
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, AppConstants.EVENT_APPROVED);
            pstmt.setString(2, AppConstants.EVENT_UPCOMING);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                int currentParticipants = rs.getInt("currentParticipants");
                Integer maxParticipants = rs.getObject("maxParticipantNumber") != null ? rs.getInt("maxParticipantNumber") : null;
                if (maxParticipants == null || currentParticipants < maxParticipants) {
                    Event event = new Event();
                    event.setEventId(rs.getInt("eventId"));
                    event.setTitle(rs.getString("title"));
                    event.setMaxParticipantNumber(maxParticipants);
                    try {
                        String startDateStr = rs.getString("startDate");
                        if(startDateStr != null) event.setStartDate(new SimpleDateFormat("yyyy-MM-dd").parse(startDateStr));
                        String endDateStr = rs.getString("endDate");
                        if(endDateStr != null) event.setEndDate(new SimpleDateFormat("yyyy-MM-dd").parse(endDateStr));
                    } catch (ParseException e) { e.printStackTrace(); }
                    event.setEmergencyLevel(rs.getString("emergencyLevel"));
                    event.setDescription(rs.getString("description"));
                    event.setOrganizer(rs.getString("organizer"));
                    event.setStatus(rs.getString("status"));
                    loadEventSkills(conn, event);
                    events.add(event);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return events;
    }
    
    private void loadEventSkills(Connection conn, Event event) throws SQLException {
        String sql = "SELECT s.skill FROM EventSkills es JOIN Skills s ON es.skillId = s.skillId WHERE es.eventId = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, event.getEventId());
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    event.addRequiredSkill(rs.getString("skill"));
                }
            }
        }
    }
    
    public List<EventParticipantDetails> getEventParticipantDetailsList(int eventId) {
        List<EventParticipantDetails> participants = new ArrayList<>();
        String sql = "SELECT SU.username AS volunteerUsername, V.fullName AS volunteerFullName, EP.hoursParticipated, EP.ratingByOrg FROM EventParticipants EP JOIN Volunteer V ON EP.username = V.username JOIN SystemUser SU ON V.username = SU.username WHERE EP.eventId = ?;";
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, eventId);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                EventParticipantDetails details = new EventParticipantDetails();
                details.setVolunteerUsername(rs.getString("volunteerUsername"));
                details.setVolunteerFullName(rs.getString("volunteerFullName"));
                if (rs.getObject("hoursParticipated") != null) details.setHoursParticipated(rs.getInt("hoursParticipated"));
                if (rs.getObject("ratingByOrg") != null) details.setRatingByOrg(rs.getInt("ratingByOrg"));
                participants.add(details);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return participants;
    }

    public List<Event> getAllEvents() {
        List<Event> events = new ArrayList<>();
        String sql = "SELECT * FROM Events";
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                Event event = new Event();
                int eventId = rs.getInt("eventId");
                event.setEventId(eventId);
                event.setTitle(rs.getString("title"));
                event.setMaxParticipantNumber(rs.getInt("maxParticipantNumber"));
                 try {
                    String startDateStr = rs.getString("startDate");
                    if(startDateStr != null) event.setStartDate(new SimpleDateFormat("yyyy-MM-dd").parse(startDateStr));
                    String endDateStr = rs.getString("endDate");
                    if(endDateStr != null) event.setEndDate(new SimpleDateFormat("yyyy-MM-dd").parse(endDateStr));
                } catch (ParseException e) { e.printStackTrace(); }
                event.setEmergencyLevel(rs.getString("emergencyLevel"));
                event.setDescription(rs.getString("description"));
                event.setOrganizer(rs.getString("organizer"));
                event.setRequestId(rs.getString("RequestId"));
                event.setStatus(rs.getString("status"));
                loadEventSkills(conn, event);
                events.add(event);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return events;
    }
} 